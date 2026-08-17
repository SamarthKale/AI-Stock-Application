package com.stockpredictor.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class HomeUiData(
    val watchlistStocks: List<Stock>,
    val topGainers: List<Stock>,
    val topLosers: List<Stock>,
)

class HomeViewModel : ViewModel() {
    // Local seed only — Watchlist screen owns its own independent in-memory list in
    // Phase 1 (Phase 2's WatchlistDao becomes the single shared source of truth).
    private val watchlistSymbols = MockStocks.all.take(5).map { it.symbol }.toSet()
    private val _data = MutableStateFlow(buildData())

    val uiState: StateFlow<UiState<HomeUiData>> = debugAwareUiState(
        dataFlow = _data,
        isEmpty = { it.watchlistStocks.isEmpty() && it.topGainers.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private fun buildData(): HomeUiData {
        val watchlistStocks = MockStocks.all.filter { it.symbol in watchlistSymbols }
        val sortedByChange = MockStocks.all.sortedByDescending { it.changePercent }
        return HomeUiData(
            watchlistStocks = watchlistStocks,
            topGainers = sortedByChange.take(3),
            topLosers = sortedByChange.takeLast(3).reversed(),
        )
    }
}
