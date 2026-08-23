package com.stockpredictor.app.data.repository

import android.content.Context
import com.stockpredictor.app.data.local.dao.ChatMessageDao
import com.stockpredictor.app.data.local.dao.SettingsDao
import com.stockpredictor.app.data.local.entity.ChatMessageEntity
import com.stockpredictor.app.data.local.entity.ChatRole
import com.stockpredictor.app.data.remote.api.BackendApiService
import com.stockpredictor.app.data.remote.api.BackendRetrofitClient
import com.stockpredictor.app.data.remote.api.ChatMessageRequest
import java.util.UUID

private const val CONVERSATION_ID_KEY = "chatbot_conversation_id"

/**
 * Android -> Spring Boot -> Gemini -> reply (Phase 5b plan section 6). A single, ongoing
 * conversation per device — a stable conversationId is generated once and persisted via the
 * existing [SettingsDao] (not a multi-thread chat list). The backend proxy is stateless
 * ([com.stockpredictor.backend.chatbot.ChatbotService] holds no chat history); [ChatMessageDao]
 * (Phase 2's DB, extended in Phase 5b) is the only place a conversation is actually kept, so
 * history survives an app restart with no server round-trip needed to reload it.
 */
class ChatbotRepository(
    context: Context,
    private val chatMessageDao: ChatMessageDao = ChatMessageDao(context.applicationContext),
    private val settingsDao: SettingsDao = SettingsDao(context.applicationContext),
    private val api: BackendApiService = BackendRetrofitClient.backendApi,
) {
    suspend fun getConversationId(): String {
        settingsDao.getString(CONVERSATION_ID_KEY)?.let { return it }
        val fresh = UUID.randomUUID().toString()
        settingsDao.setString(CONVERSATION_ID_KEY, fresh)
        return fresh
    }

    suspend fun getHistory(conversationId: String): List<ChatMessageEntity> =
        chatMessageDao.getBySession(conversationId)

    /** Writes the user's message first, then the assistant's reply once it arrives. If the
     *  backend call throws, the user's message is already persisted — callers should reload
     *  history to show it rather than losing it, then surface the failure separately. */
    suspend fun sendMessage(conversationId: String, text: String) {
        chatMessageDao.insert(conversationId, ChatRole.USER, text, System.currentTimeMillis())
        val response = api.sendChatMessage(ChatMessageRequest(conversationId, text))
        chatMessageDao.insert(conversationId, ChatRole.ASSISTANT, response.reply, System.currentTimeMillis())
    }
}
