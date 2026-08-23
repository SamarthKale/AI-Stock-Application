package com.stockpredictor.app.ui.screens.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.entity.ChatMessageEntity
import com.stockpredictor.app.data.repository.ChatbotRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single ongoing conversation per device ([ChatbotRepository] persists one stable
 * conversationId). Mirrors [com.stockpredictor.app.ui.screens.predictions.PredictionsViewModel]'s
 * job-cancel/try-catch/[CancellationException]-rethrow pattern for the initial history load.
 * [sendMessage] keeps existing history visible even if a send fails — the user's own message is
 * already persisted by the repository before the network call, so a failure is surfaced via
 * [sendError] (a small, dismissible signal) rather than replacing [uiState] with a whole-screen
 * [UiState.Error], the same "additive, not load-bearing" philosophy CryptoDetailViewModel already
 * uses for its prediction section.
 */
class ChatbotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatbotRepository(application)

    private var conversationId: String? = null
    private val _realState = MutableStateFlow<UiState<List<ChatMessageEntity>>>(UiState.Loading)
    private val _isSending = MutableStateFlow(false)
    private val _sendError = MutableStateFlow<String?>(null)
    private var loadJob: Job? = null

    val uiState: StateFlow<UiState<List<ChatMessageEntity>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val id = repository.getConversationId()
                conversationId = id
                val history = repository.getHistory(id)
                _realState.value = if (history.isEmpty()) UiState.Empty else UiState.Success(history)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isSending.value) return
        val id = conversationId ?: return
        viewModelScope.launch {
            _isSending.value = true
            _sendError.value = null
            try {
                repository.sendMessage(id, trimmed)
                _realState.value = UiState.Success(repository.getHistory(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The user's own message is already persisted (ChatbotRepository writes it before
                // calling the backend) -- reload so it still shows even though the reply failed.
                val history = repository.getHistory(id)
                if (history.isNotEmpty()) _realState.value = UiState.Success(history)
                _sendError.value = e.toUserMessage()
            } finally {
                _isSending.value = false
            }
        }
    }

    fun dismissSendError() {
        _sendError.value = null
    }
}
