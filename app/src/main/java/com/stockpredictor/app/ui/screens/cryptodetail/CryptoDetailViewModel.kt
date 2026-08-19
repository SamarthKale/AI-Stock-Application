package com.stockpredictor.app.ui.screens.cryptodetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.WatchlistDao
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.data.repository.CoinNotFoundException
import com.stockpredictor.app.data.repository.CoinRepository
import com.stockpredictor.app.data.repository.PredictionRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CryptoDetailData(
    val coin: Coin,
    val prediction: Prediction?,
    val isInWatchlist: Boolean,
)

/**
 * Watchlist membership is backed by [WatchlistDao]; the prediction comes from
 * [PredictionRepository] (Phase 5 — Android -> Spring Boot -> FastAPI, with a
 * CachedPredictionDao fallback on failure) instead of mock data. Both are the same shared
 * source of truth the Watchlist/Home tabs read, so a toggle here shows up everywhere else
 * immediately. The coin itself comes from [CoinRepository] (Phase 4), so it can genuinely be
 * Loading/Error/retried like any other network-backed screen. A prediction fetch failure is
 * additive, not load-bearing (per Phase 4/5's design) — [_prediction] just stays null and
 * CryptoDetailScreen's existing small, scoped "no prediction available" state handles it,
 * rather than surfacing a whole-screen error for a secondary card.
 */
class CryptoDetailViewModel(
    application: Application,
    private val coinId: String,
) : AndroidViewModel(application) {
    private val watchlistDao = WatchlistDao(application)
    private val syncRepository = FirestoreSyncRepository.getInstance(application)
    private val authRepository = FirebaseAuthRepository()
    private val coinRepository = CoinRepository.getInstance(application)
    private val predictionRepository = PredictionRepository(application)

    private val _coinState = MutableStateFlow<UiState<Coin>>(UiState.Loading)
    private val _prediction = MutableStateFlow<Prediction?>(null)
    private val _isInWatchlist = MutableStateFlow(false)

    val uiState: StateFlow<UiState<CryptoDetailData>> = debugAwareUiState(
        combine(_coinState, _prediction, _isInWatchlist) { coinState, prediction, inWatchlist ->
            when (coinState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Empty -> UiState.Empty
                is UiState.Error -> coinState
                is UiState.Success -> UiState.Success(CryptoDetailData(coinState.data, prediction, inWatchlist))
            }
        },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        fetchCoin()
        fetchPrediction()
        viewModelScope.launch {
            _isInWatchlist.value = watchlistDao.isWatchlisted(coinId)
        }
    }

    private fun fetchCoin() {
        viewModelScope.launch {
            _coinState.value = UiState.Loading
            try {
                val result = coinRepository.getCoinDetail(coinId)
                _coinState.value = UiState.Success(result.data)
            } catch (e: CoinNotFoundException) {
                _coinState.value = UiState.Empty
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _coinState.value = UiState.Error(e.toUserMessage(), ::fetchCoin)
            }
        }
    }

    private fun fetchPrediction() {
        viewModelScope.launch {
            _prediction.value = try {
                predictionRepository.getPrediction(coinId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            val current = (_coinState.value as? UiState.Success)?.data
            if (_isInWatchlist.value) {
                syncRepository.removeFromWatchlist(uid, coinId)
            } else if (current != null) {
                syncRepository.addToWatchlist(uid, current.id, current.symbol, current.name, current.image)
            }
            _isInWatchlist.value = watchlistDao.isWatchlisted(coinId)
        }
    }
}

class CryptoDetailViewModelFactory(
    private val application: Application,
    private val coinId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CryptoDetailViewModel(application, coinId) as T
}
