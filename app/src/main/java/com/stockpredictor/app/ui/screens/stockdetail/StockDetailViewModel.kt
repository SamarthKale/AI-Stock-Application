package com.stockpredictor.app.ui.screens.stockdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockPredictions
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StockDetailData(
    val stock: Stock,
    val prediction: Prediction?,
    val isInWatchlist: Boolean,
)

private data class DetailPayload(val stock: Stock?, val prediction: Prediction?, val isInWatchlist: Boolean)

class StockDetailViewModel(private val symbol: String) : ViewModel() {
    private val stock = MockStocks.findBySymbol(symbol)
    private val prediction = MockPredictions.forSymbol(symbol)
    private val _isInWatchlist = MutableStateFlow(false)

    val uiState: StateFlow<UiState<StockDetailData>> = debugAwareUiState(
        dataFlow = _isInWatchlist.map { DetailPayload(stock, prediction, it) },
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

    fun toggleWatchlist() {
        _isInWatchlist.value = !_isInWatchlist.value
    }
}

class StockDetailViewModelFactory(private val symbol: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StockDetailViewModel(symbol) as T
}
