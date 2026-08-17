package com.stockpredictor.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.model.WatchlistItem
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** In-memory only in Phase 1; Phase 2's WatchlistDao.updateSortOrders(...) replaces this store. */
class WatchlistViewModel : ViewModel() {
    private val _items = MutableStateFlow(seedItems())

    val uiState: StateFlow<UiState<List<Stock>>> = debugAwareUiState(
        dataFlow = _items.map { items -> items.sortedBy { it.sortOrder }.mapNotNull { MockStocks.findBySymbol(it.symbol) } },
        isEmpty = { it.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun remove(symbol: String) {
        _items.update { list -> list.filterNot { it.symbol == symbol } }
    }

    fun moveUp(symbol: String) = reorder(symbol, -1)
    fun moveDown(symbol: String) = reorder(symbol, 1)

    private fun reorder(symbol: String, delta: Int) {
        _items.update { list ->
            val sorted = list.sortedBy { it.sortOrder }.toMutableList()
            val index = sorted.indexOfFirst { it.symbol == symbol }
            if (index == -1) return@update list
            val newIndex = (index + delta).coerceIn(0, sorted.lastIndex)
            if (index == newIndex) return@update list
            val item = sorted.removeAt(index)
            sorted.add(newIndex, item)
            sorted.mapIndexed { i, w -> w.copy(sortOrder = i) }
        }
    }

    private fun seedItems(): List<WatchlistItem> =
        MockStocks.all.take(5).mapIndexed { index, stock ->
            WatchlistItem(symbol = stock.symbol, addedAt = System.currentTimeMillis(), sortOrder = index)
        }
}
