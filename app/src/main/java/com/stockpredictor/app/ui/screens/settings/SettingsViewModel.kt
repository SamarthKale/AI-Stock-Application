package com.stockpredictor.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.SettingsDao
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.debug.DebugStateController
import com.stockpredictor.app.debug.DebugUiMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiData(
    val notificationsEnabled: Boolean,
    val debugMode: DebugUiMode,
    val userEmail: String?,
)

private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

/**
 * Deliberately always renders its real content (never Loading/Empty/Error) — this is the
 * screen that drives DebugStateController, so if it also obeyed the toggle it could hide
 * its own controls and strand the tester in a forced state with no way back.
 *
 * The notifications toggle persists via [SettingsDao] (Phase 2). The debug UI-state toggle
 * stays in-memory only (DebugStateController) — it's a dev tool, not user data.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = SettingsDao(application)
    private val authRepository = FirebaseAuthRepository()
    private val syncRepository = FirestoreSyncRepository.getInstance(application)
    private val _notificationsEnabled = MutableStateFlow(true)

    private val _logoutEvent = MutableSharedFlow<Unit>()
    /** Emitted once sign-out (listener stop + local cache clear + Firebase sign-out) has
     *  fully completed — the screen navigates on this, not on the button click itself, so
     *  navigation can never race ahead of the cache clear. */
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    val uiState: StateFlow<SettingsUiData> = combine(
        _notificationsEnabled, DebugStateController.mode, authRepository.authStateFlow().map { it?.email },
    ) { enabled, mode, userEmail ->
        SettingsUiData(enabled, mode, userEmail)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiData(true, DebugUiMode.NONE, authRepository.currentUser?.email),
    )

    init {
        viewModelScope.launch {
            _notificationsEnabled.value = dao.getBoolean(KEY_NOTIFICATIONS_ENABLED, default = true)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        viewModelScope.launch { dao.setBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun setDebugMode(mode: DebugUiMode) {
        DebugStateController.setMode(mode)
    }

    /**
     * Stops the Firestore listener and wipes the local watchlist cache before signing out —
     * in that order — so a different account signing in on this device never inherits either
     * the previous user's listener or their cached local rows (see FirestoreSyncRepository).
     * Runs as a suspend sequence and only signals [logoutEvent] once every step has finished,
     * so the screen never navigates to Login while the clear is still in flight.
     */
    fun logout() {
        viewModelScope.launch {
            syncRepository.stopListening()
            syncRepository.clearLocalCache()
            authRepository.signOut()
            _logoutEvent.emit(Unit)
        }
    }
}
