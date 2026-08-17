package com.stockpredictor.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.debug.DebugStateController
import com.stockpredictor.app.debug.DebugUiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiData(
    val notificationsEnabled: Boolean,
    val debugMode: DebugUiMode,
)

/**
 * Deliberately always renders its real content (never Loading/Empty/Error) — this is the
 * screen that drives DebugStateController, so if it also obeyed the toggle it could hide
 * its own controls and strand the tester in a forced state with no way back.
 */
class SettingsViewModel : ViewModel() {
    private val _notificationsEnabled = MutableStateFlow(true)

    val uiState: StateFlow<SettingsUiData> = combine(
        _notificationsEnabled, DebugStateController.mode,
    ) { enabled, mode ->
        SettingsUiData(enabled, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiData(true, DebugUiMode.NONE))

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setDebugMode(mode: DebugUiMode) {
        DebugStateController.setMode(mode)
    }
}
