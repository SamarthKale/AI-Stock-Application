package com.stockpredictor.app.ui.screens.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.WatchlistDao
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.data.repository.CoinRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backed by [WatchlistDao] for coin-id order and [FirestoreSyncRepository] for writes
 *  (Phase 2.5) — the SQLite table is the offline cache/order source, Firestore is the synced
 *  source of truth, and [CoinRepository] fills in each coin's live price data (Phase 4). */
class WatchlistViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WatchlistDao(application)
    private val syncRepository = FirestoreSyncRepository.getInstance(application)
    private val authRepository = FirebaseAuthRepository()
    private val coinRepository = CoinRepository.getInstance(application)

    private val _realState = MutableStateFlow<UiState<List<Coin>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Coin>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // See HomeViewModel's identical field for why: cancels a still-running previous refresh so
    // an older, slower call can't overwrite _realState after a newer one already completed.
    private var refreshJob: Job? = null

    init {
        // Deliberately NOT calling refresh() here — see HomeViewModel's init comment. The
        // screen's LaunchedEffect(Unit) is the sole initial-load trigger; only the sync-change
        // subscription belongs in init.
        viewModelScope.launch {
            FirestoreSyncRepository.changes.collect { refresh() }
        }
    }

    /** Re-queries the DB then batches one /coins/markets?ids=... call for the whole list.
     *  Called from the screen's LaunchedEffect(Unit) (first composition and every re-entry to
     *  the tab) and on the shared sync-change signal, so edits made elsewhere (e.g. Crypto
     *  Detail's watchlist toggle) show up when revisited. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val ids = dao.getAll().map { it.coinId }
            if (ids.isEmpty()) {
                _realState.value = UiState.Empty
                return@launch
            }
            _realState.value = UiState.Loading
            try {
                val result = coinRepository.getMarkets(ids)
                // Preserve DAO sort_order — the network response isn't guaranteed to echo
                // request order back.
                val byId = result.data.associateBy { it.id }
                val ordered = ids.mapNotNull { byId[it] }
                _realState.value = if (ordered.isEmpty()) UiState.Empty else UiState.Success(ordered)
            } catch (e: CancellationException) {
                throw e // superseded by a newer refresh() — see HomeViewModel's identical guard
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }

    fun remove(coinId: String) {
        viewModelScope.launch {
            syncRepository.removeFromWatchlist(authRepository.currentUser?.uid, coinId)
        }
    }

    fun moveUp(coinId: String) = reorder(coinId, -1)
    fun moveDown(coinId: String) = reorder(coinId, 1)

    private fun reorder(coinId: String, delta: Int) {
        viewModelScope.launch {
            val current = (_realState.value as? UiState.Success)?.data?.map { it.id } ?: return@launch
            val index = current.indexOf(coinId)
            if (index == -1) return@launch
            val newIndex = (index + delta).coerceIn(0, current.lastIndex)
            if (index == newIndex) return@launch
            val reordered = current.toMutableList().apply { add(newIndex, removeAt(index)) }
            syncRepository.reorderWatchlist(authRepository.currentUser?.uid, reordered)
        }
    }
}
