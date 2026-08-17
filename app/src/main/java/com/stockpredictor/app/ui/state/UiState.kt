package com.stockpredictor.app.ui.state

/**
 * Shared UI state shape for every data-driven screen's ViewModel. This is the seam
 * Phase 2 (SQLite) and Phase 4 (Retrofit) plug into: the composable only ever renders
 * from this sealed class, so swapping the data source never touches screen code.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data object Empty : UiState<Nothing>()
    data class Error(val message: String, val retry: (() -> Unit)? = null) : UiState<Nothing>()
}
