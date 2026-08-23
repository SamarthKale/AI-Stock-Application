package com.stockpredictor.app.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.NotificationDao
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.NotificationItem
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Phase 5c: real alert history from [NotificationDao] (populated by
 *  StockPredictorFcmService.onMessageReceived), replacing mock/MockNotifications.kt. */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = NotificationDao(application)

    private val _realState = MutableStateFlow<UiState<List<NotificationItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<NotificationItem>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val items = dao.getAll()
                _realState.value = if (items.isEmpty()) UiState.Empty else UiState.Success(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            dao.markRead(id)
            refresh()
        }
    }
}
