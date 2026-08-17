package com.stockpredictor.app.ui.screens.predictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockPredictions
import com.stockpredictor.app.mock.MockStocks
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class PredictionFilter { ALL, UP, DOWN, FLAT }

data class PredictionRow(val stock: Stock, val prediction: Prediction)

class PredictionsViewModel : ViewModel() {
    private val _filter = MutableStateFlow(PredictionFilter.ALL)
    val filter: StateFlow<PredictionFilter> = _filter.asStateFlow()

    private val allRows: List<PredictionRow> = MockPredictions.all.mapNotNull { prediction ->
        MockStocks.findBySymbol(prediction.symbol)?.let { PredictionRow(it, prediction) }
    }

    private val rows: Flow<List<PredictionRow>> = _filter.map { filter ->
        val filtered = when (filter) {
            PredictionFilter.ALL -> allRows
            PredictionFilter.UP -> allRows.filter { it.prediction.direction == PredictionDirection.Up }
            PredictionFilter.DOWN -> allRows.filter { it.prediction.direction == PredictionDirection.Down }
            PredictionFilter.FLAT -> allRows.filter { it.prediction.direction == PredictionDirection.Flat }
        }
        filtered.sortedByDescending { it.prediction.confidence }
    }

    val uiState: StateFlow<UiState<List<PredictionRow>>> = debugAwareUiState(
        dataFlow = rows,
        isEmpty = { it.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setFilter(value: PredictionFilter) {
        _filter.value = value
    }
}
