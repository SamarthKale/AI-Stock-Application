package com.stockpredictor.app.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.local.dao.RecentSearchDao
import com.stockpredictor.app.mock.MockStocks
import kotlinx.coroutines.FlowPreview
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = RecentSearchDao(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val results: Flow<List<Stock>> = _query
        .debounce(300)
        .map { q ->
            if (q.isBlank()) {
                emptyList()
            } else {
                MockStocks.all.filter { it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
            }
        }

    val uiState: StateFlow<UiState<List<Stock>>> = debugAwareUiState(
        dataFlow = results,
        isEmpty = { it.isEmpty() },
        errorMessage = "Search failed. Please try again.",
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        refreshRecentSearches()
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
