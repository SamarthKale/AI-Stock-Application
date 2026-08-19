package com.stockpredictor.app.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.RecentSearchDao
import com.stockpredictor.app.data.repository.CoinSearchRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.model.CoinSearchResult
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = RecentSearchDao(application)
    private val coinSearchRepository = CoinSearchRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _realState = MutableStateFlow<UiState<List<CoinSearchResult>>>(UiState.Empty)
    val uiState: StateFlow<UiState<List<CoinSearchResult>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refreshRecentSearches()
        viewModelScope.launch {
            // collectLatest (not map/mapLatest into the data flow) so a query typed while a
            // search is in flight cancels the stale request rather than racing it — matches
            // the 300ms debounce's intent of "only the latest query wins."
            _query.debounce(300).collectLatest { q -> runSearch(q) }
        }
    }

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) {
            _realState.value = UiState.Empty
            return
        }
        _realState.value = UiState.Loading
        try {
            val hits = coinSearchRepository.search(query)
            _realState.value = if (hits.isEmpty()) UiState.Empty else UiState.Success(hits)
        } catch (e: CancellationException) {
            // collectLatest cancels this exact coroutine when a newer query arrives — rethrow
            // rather than reporting a spurious Error for a search that was simply superseded.
            throw e
        } catch (e: Exception) {
            _realState.value = UiState.Error(e.toUserMessage()) { retrySearch() }
        }
    }

    private fun retrySearch() {
        viewModelScope.launch { runSearch(_query.value) }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Record a search on submit (not per keystroke); DAO re-timestamps instead of duplicating. */
    fun onSearchSubmit(query: String = _query.value) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            dao.recordSearch(trimmed)
            refreshRecentSearches()
        }
    }

    private fun refreshRecentSearches() {
        viewModelScope.launch {
            _recentSearches.value = dao.getAll().map { it.query }
        }
    }
}
