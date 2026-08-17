package com.stockpredictor.app.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DebugUiMode { NONE, LOADING, EMPTY, ERROR }

/**
 * App-wide switch that lets Settings force any data-driven screen's [UiState][com.stockpredictor.app.ui.state.UiState]
 * into Loading/Empty/Error, regardless of the underlying mock data, so every state is
 * visually verifiable without a throwaway test harness (Phase 1, Task 7).
 *
 * Deliberately NOT applied to Settings itself, or to Onboarding/Login/Signup/ForgotPassword:
 * those screens have no real "empty/error" fetch concept, and forcing Settings into a
 * non-Success state would hide the very toggle needed to turn it back off. See the
 * per-screen ViewModel comments for where this is (and isn't) wired in.
 */
object DebugStateController {
    private val _mode = MutableStateFlow(DebugUiMode.NONE)
    val mode: StateFlow<DebugUiMode> = _mode.asStateFlow()

    fun setMode(mode: DebugUiMode) {
        _mode.value = mode
    }
}
