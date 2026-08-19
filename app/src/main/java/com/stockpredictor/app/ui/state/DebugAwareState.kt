package com.stockpredictor.app.ui.state

import com.stockpredictor.app.debug.DebugStateController
import com.stockpredictor.app.debug.DebugUiMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Wraps [dataFlow] so it reacts to [DebugStateController], letting Settings force any
 * screen using this into Loading/Empty/Error on top of whatever the real mock data is.
 */
fun <T> debugAwareUiState(
    dataFlow: Flow<T>,
    isEmpty: (T) -> Boolean,
    errorMessage: String = "Something went wrong. Please try again.",
    onRetry: (() -> Unit)? = null,
): Flow<UiState<T>> = combine(dataFlow, DebugStateController.mode) { data, mode ->
    when (mode) {
        DebugUiMode.LOADING -> UiState.Loading
        DebugUiMode.EMPTY -> UiState.Empty
        DebugUiMode.ERROR -> UiState.Error(errorMessage, onRetry)
        DebugUiMode.NONE -> if (isEmpty(data)) UiState.Empty else UiState.Success(data)
    }
}

/**
 * Overload for screens whose ViewModel already computes real Loading/Success/Empty/Error
 * (Phase 4's network-backed screens) — [realState] is passed through untouched when the debug
 * mode is NONE, and overridden otherwise, so Settings' debug toggle still works on top of a
 * real data source exactly as it does for the plain-[Flow]-of-data overload above.
 */
fun <T> debugAwareUiState(
    realState: Flow<UiState<T>>,
): Flow<UiState<T>> = combine(realState, DebugStateController.mode) { real, mode ->
    when (mode) {
        DebugUiMode.LOADING -> UiState.Loading
        DebugUiMode.EMPTY -> UiState.Empty
        DebugUiMode.ERROR -> UiState.Error("Something went wrong. Please try again.", null)
        DebugUiMode.NONE -> real
    }
}
