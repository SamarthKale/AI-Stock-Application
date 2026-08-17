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
