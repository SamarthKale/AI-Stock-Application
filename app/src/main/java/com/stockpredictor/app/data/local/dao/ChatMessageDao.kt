package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.ChatMessageTable
import com.stockpredictor.app.data.local.entity.ChatMessageEntity
import com.stockpredictor.app.data.local.entity.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full CRUD against the `chat_messages` table. Never call raw SQL from outside this class. */
class ChatMessageDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    /** Chronological (oldest first) — the order ChatbotScreen renders the conversation in. */
    suspend fun getBySession(conversationId: String): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            ChatMessageTable.NAME, null, "${ChatMessageTable.COL_CONVERSATION_ID}=?", arrayOf(conversationId),
            null, null, "${ChatMessageTable.COL_TIMESTAMP} ASC",
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntity()) }
        }
    }

    suspend fun insert(conversationId: String, role: ChatRole, content: String, timestamp: Long): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(ChatMessageTable.COL_CONVERSATION_ID, conversationId)
                put(ChatMessageTable.COL_ROLE, role.toStorageString())
                put(ChatMessageTable.COL_CONTENT, content)
                put(ChatMessageTable.COL_TIMESTAMP, timestamp)
            }
            dbHelper.writableDatabase.insert(ChatMessageTable.NAME, null, values)
        }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(ChatMessageTable.NAME, "${ChatMessageTable.COL_ID}=?", arrayOf(id.toString()))
        Unit
    }

    private fun ChatRole.toStorageString(): String = when (this) {
        ChatRole.USER -> "USER"
        ChatRole.ASSISTANT -> "ASSISTANT"
    }

    private fun String.toChatRole(): ChatRole = when (this) {
        "USER" -> ChatRole.USER
        else -> ChatRole.ASSISTANT
    }

    private fun Cursor.toEntity(): ChatMessageEntity = ChatMessageEntity(
        id = getLong(getColumnIndexOrThrow(ChatMessageTable.COL_ID)),
        conversationId = getString(getColumnIndexOrThrow(ChatMessageTable.COL_CONVERSATION_ID)),
        role = getString(getColumnIndexOrThrow(ChatMessageTable.COL_ROLE)).toChatRole(),
        content = getString(getColumnIndexOrThrow(ChatMessageTable.COL_CONTENT)),
        timestamp = getLong(getColumnIndexOrThrow(ChatMessageTable.COL_TIMESTAMP)),
    )
}
