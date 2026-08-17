package com.stockpredictor.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockNotifications
import com.stockpredictor.app.model.NotificationItem
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class NotificationsViewModel : ViewModel() {
    private val _items = MutableStateFlow(MockNotifications.all)

    val uiState: StateFlow<UiState<List<NotificationItem>>> = debugAwareUiState(
        dataFlow = _items,
        isEmpty = { it.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun markRead(id: Long) {
        _items.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
    }
}
