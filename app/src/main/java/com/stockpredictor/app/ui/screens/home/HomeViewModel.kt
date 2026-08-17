package com.stockpredictor.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.WatchlistDao
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiData(
    val watchlistStocks: List<Stock>,
    val topGainers: List<Stock>,
    val topLosers: List<Stock>,
)

/** Watchlist summary reads from [WatchlistDao] (Phase 2) — the same shared source of
 *  truth the Watchlist tab and Stock Detail's toggle write to. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WatchlistDao(application)
    private val _data = MutableStateFlow(buildData(emptySet()))

    val uiState: StateFlow<UiState<HomeUiData>> = debugAwareUiState(
        dataFlow = _data,
        isEmpty = { it.watchlistStocks.isEmpty() && it.topGainers.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refresh()
    }

    /** Called on init and again from the screen's LaunchedEffect(Unit) so a watchlist edit
     *  made on another tab is reflected here when the user returns to Home. */
    fun refresh() {
        viewModelScope.launch {
            val symbols = dao.getAll().map { it.symbol }.toSet()
            _data.value = buildData(symbols)
        }
    }

    private fun buildData(watchlistSymbols: Set<String>): HomeUiData {
        val watchlistStocks = MockStocks.all.filter { it.symbol in watchlistSymbols }
        val sortedByChange = MockStocks.all.sortedByDescending { it.changePercent }
        return HomeUiData(
            watchlistStocks = watchlistStocks,
            topGainers = sortedByChange.take(3),
            topLosers = sortedByChange.takeLast(3).reversed(),
        )
    }
}
