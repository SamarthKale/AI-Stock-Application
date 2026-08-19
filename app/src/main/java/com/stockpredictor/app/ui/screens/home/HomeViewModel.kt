package com.stockpredictor.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.WatchlistDao
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.data.repository.CoinRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.TrendingCoin
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiData(
    val watchlistCoins: List<Coin>,
    val topGainers: List<Coin>,
    val topLosers: List<Coin>,
    val trending: List<TrendingCoin>,
    val isStale: Boolean,
)

/** Watchlist summary reads from [WatchlistDao] (Phase 2) — the same shared source of
 *  truth the Watchlist tab and Crypto Detail's toggle write to. Market data comes from
 *  [CoinRepository] (Phase 4): one batched call for the watchlist's own coins, one call for
 *  the general market list gainers/losers are derived from client-side (never a dedicated
 *  gainers/losers endpoint call — see CoinRepository/the migration plan for why). */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WatchlistDao(application)
    private val coinRepository = CoinRepository.getInstance(application)

    private val _realState = MutableStateFlow<UiState<HomeUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeUiData>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // Tracks the in-flight refresh so a new call cancels the previous one instead of racing it —
    // otherwise an older (slower) refresh finishing after a newer (faster) one could overwrite
    // _realState with stale results. See the Step 3 audit's "overlapping refresh coroutines"
    // finding.
    private var refreshJob: Job? = null

    init {
        // Deliberately NOT calling refresh() here: the screen's LaunchedEffect(Unit) already
        // calls it on first composition, which happens essentially immediately after this
        // ViewModel is created. Calling it from both places used to fire two independent
        // first-load fetches in a burst on cold start (see the Step 3 audit) — only the
        // sync-change subscription belongs in init, refresh() itself is screen-triggered.
        viewModelScope.launch {
            FirestoreSyncRepository.changes.collect { refresh() }
        }
    }

    /** Called on the shared sync-change signal (Phase 2.5 background Firestore update) and from
     *  the screen's LaunchedEffect(Unit) — on first composition (the ViewModel's only "initial
     *  load" trigger, see init's comment) and again on every re-entry to the tab so a watchlist
     *  edit made elsewhere is reflected here. Cancels any still-running previous refresh first. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val watchlistEntities = dao.getAll()
                val watchlistIds = watchlistEntities.map { it.coinId }

                coroutineScope {
                    val watchlistDeferred = async {
                        if (watchlistIds.isEmpty()) null else coinRepository.getMarkets(watchlistIds)
                    }
                    val marketsDeferred = async { coinRepository.getMarkets(null) }
                    val trendingDeferred = async { coinRepository.getTrending() }

                    val watchlistResult = watchlistDeferred.await()
                    val marketsResult = marketsDeferred.await()
                    val trending = trendingDeferred.await()

                    // Preserve the DAO's own sort_order rather than whatever order the network
                    // response happens to return ids in (GET .../markets?ids=... is not
                    // guaranteed to echo request order back).
                    val byId = watchlistResult?.data?.associateBy { it.id }.orEmpty()
                    val watchlistCoins = watchlistIds.mapNotNull { byId[it] }

                    val sortedByChange = marketsResult.data.sortedByDescending { it.priceChangePercentage24h }
                    val data = HomeUiData(
                        watchlistCoins = watchlistCoins,
                        topGainers = sortedByChange.take(3),
                        topLosers = sortedByChange.takeLast(3).reversed(),
                        trending = trending,
                        isStale = (watchlistResult?.isStale ?: false) || marketsResult.isStale,
                    )
                    _realState.value =
                        if (data.watchlistCoins.isEmpty() && data.topGainers.isEmpty()) UiState.Empty else UiState.Success(data)
                }
            } catch (e: CancellationException) {
                // Rethrow, never swallow: this refresh was superseded by a newer call (see
                // refreshJob above) — letting cancellation propagate normally means this
                // coroutine stops here instead of falling into the catch below and overwriting
                // _realState with a spurious Error right as (or after) the newer call succeeds.
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }
}
