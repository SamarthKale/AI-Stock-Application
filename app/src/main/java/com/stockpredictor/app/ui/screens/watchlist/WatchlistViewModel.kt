package com.stockpredictor.app.ui.screens.watchlist

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backed by [WatchlistDao] (Phase 2) — the SQLite table is the single shared source of truth. */
class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WatchlistDao(application)
    private val _symbols = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<UiState<List<Stock>>> = debugAwareUiState(
        dataFlow = _symbols.map { symbols -> symbols.mapNotNull { MockStocks.findBySymbol(it) } },
        isEmpty = { it.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refresh()
    }

    /** Re-queries the DB. Called on init and again from the screen's LaunchedEffect(Unit) so
     *  edits made elsewhere (e.g. Stock Detail's watchlist toggle) show up when this tab is revisited. */
    fun refresh() {
        viewModelScope.launch { _symbols.value = dao.getAll().map { it.symbol } }
    }

    fun remove(symbol: String) {
        viewModelScope.launch {
            dao.delete(symbol)
            refresh()
        }
    }

    fun moveUp(symbol: String) = reorder(symbol, -1)
    fun moveDown(symbol: String) = reorder(symbol, 1)

    private fun reorder(symbol: String, delta: Int) {
        viewModelScope.launch {
            val current = _symbols.value
            val index = current.indexOf(symbol)
            if (index == -1) return@launch
            val newIndex = (index + delta).coerceIn(0, current.lastIndex)
            if (index == newIndex) return@launch
            val reordered = current.toMutableList().apply { add(newIndex, removeAt(index)) }
            dao.updateSortOrders(reordered)
            refresh()
        }
    }
}
