package com.stockpredictor.app.ui.screens.stockdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.CachedPredictionDao
import com.stockpredictor.app.data.local.dao.WatchlistDao
import com.stockpredictor.app.mock.MockPredictions
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StockDetailData(
    val stock: Stock,
    val prediction: Prediction?,
    val isInWatchlist: Boolean,
)

private data class DetailPayload(val stock: Stock?, val prediction: Prediction?, val isInWatchlist: Boolean)

/**
 * Watchlist membership is backed by [WatchlistDao] and the prediction is a read-through
 * [CachedPredictionDao] cache (Phase 2) — both are the same shared source of truth the
 * Watchlist/Home tabs read, so a toggle here shows up everywhere else immediately.
 */
class StockDetailViewModel(
    application: Application,
    private val symbol: String,
) : AndroidViewModel(application) {
    private val watchlistDao = WatchlistDao(application)
    private val cachedPredictionDao = CachedPredictionDao(application)

    private val stock = MockStocks.findBySymbol(symbol)
    private val _prediction = MutableStateFlow(MockPredictions.forSymbol(symbol))
    private val _isInWatchlist = MutableStateFlow(false)

    val uiState: StateFlow<UiState<StockDetailData>> = debugAwareUiState(
        dataFlow = combine(_prediction, _isInWatchlist) { prediction, inWatchlist ->
            DetailPayload(stock, prediction, inWatchlist)
        },
        isEmpty = { it.stock == null },
    ).map { state ->
        // isEmpty above guarantees stock is non-null whenever NONE-mode produces Success.
        if (state is UiState.Success) {
            UiState.Success(StockDetailData(state.data.stock!!, state.data.prediction, state.data.isInWatchlist))
        } else {
            @Suppress("UNCHECKED_CAST")
            state as UiState<StockDetailData>
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        viewModelScope.launch {
            _isInWatchlist.value = watchlistDao.isWatchlisted(symbol)

            // Read-through cache: a previously cached row wins (works offline, later phases'
            // fetch may be unavailable); otherwise persist the current mock "fetch" result so
            // it's available next time.
            val cached = cachedPredictionDao.getBySymbol(symbol)
            if (cached != null) {
                _prediction.value = Prediction(
                    symbol = cached.symbol,
                    confidence = cached.confidence,
                    direction = cached.direction,
                    targetPrice = cached.targetPrice,
                    horizon = cached.horizon,
                    generatedAt = cached.generatedAt,
                )
            } else {
                _prediction.value?.let { prediction ->
                    cachedPredictionDao.upsert(
                        symbol = prediction.symbol,
                        confidence = prediction.confidence,
                        direction = prediction.direction,
                        targetPrice = prediction.targetPrice,
                        horizon = prediction.horizon,
                        generatedAt = prediction.generatedAt,
                    )
                }
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                watchlistDao.delete(symbol)
            } else {
                watchlistDao.insert(symbol)
            }
            _isInWatchlist.value = watchlistDao.isWatchlisted(symbol)
        }
    }
}

class StockDetailViewModelFactory(
    private val application: Application,
    private val symbol: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StockDetailViewModel(application, symbol) as T
}
