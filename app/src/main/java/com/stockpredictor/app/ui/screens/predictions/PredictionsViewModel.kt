package com.stockpredictor.app.ui.screens.predictions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.repository.CoinRepository
import com.stockpredictor.app.data.repository.PredictionRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_COINS = 12 // matches the original 12-coin mock scale (Phase 5 plan)

enum class PredictionFilter { ALL, UP, DOWN, FLAT }

data class PredictionRow(val coin: Coin, val prediction: Prediction)

/**
 * Real predictions (Phase 5) for the top coins by market cap — reuses [CoinRepository]'s
 * already-cached market list rather than a hardcoded coin set, so this list tracks whatever the
 * rest of the app currently shows. Per-coin prediction failures are skipped individually (a
 * coin without a prediction just doesn't appear in the list) rather than failing the whole
 * screen — predictions are additive, matching Crypto Detail's same failure handling.
 */
class PredictionsViewModel(application: Application) : AndroidViewModel(application) {
    private val coinRepository = CoinRepository.getInstance(application)
    private val predictionRepository = PredictionRepository(application)

    private val _filter = MutableStateFlow(PredictionFilter.ALL)
    val filter: StateFlow<PredictionFilter> = _filter.asStateFlow()

    private val _realState = MutableStateFlow<UiState<List<PredictionRow>>>(UiState.Loading)
    private var loadJob: Job? = null

    val uiState: StateFlow<UiState<List<PredictionRow>>> = debugAwareUiState(
        combine(_realState, _filter) { state, filter ->
            if (state !is UiState.Success) return@combine state
            val filtered = when (filter) {
                PredictionFilter.ALL -> state.data
                PredictionFilter.UP -> state.data.filter { it.prediction.direction == PredictionDirection.Up }
                PredictionFilter.DOWN -> state.data.filter { it.prediction.direction == PredictionDirection.Down }
                PredictionFilter.FLAT -> state.data.filter { it.prediction.direction == PredictionDirection.Flat }
            }.sortedByDescending { it.prediction.confidence }
            if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
        },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val coins = coinRepository.getMarkets(null).data.take(MAX_COINS)
                val rows = coroutineScope {
                    coins.map { coin ->
                        async {
                            try {
                                PredictionRow(coin, predictionRepository.getPrediction(coin.id))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.mapNotNull { it.await() }
                }
                _realState.value = if (rows.isEmpty()) UiState.Empty else UiState.Success(rows)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }

    fun setFilter(value: PredictionFilter) {
        _filter.value = value
    }
}
