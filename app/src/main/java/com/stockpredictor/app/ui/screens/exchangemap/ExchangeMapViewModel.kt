package com.stockpredictor.app.ui.screens.exchangemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.repository.ExchangeMarketInfo
import com.stockpredictor.app.data.repository.ExchangeRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** No [android.app.Application] dependency needed -- [ExchangeRepository] only talks to CoinGecko,
 *  no SQLite/Context involved -- so a plain [ViewModel] is enough, unlike most other ViewModels
 *  in this app. */
class ExchangeMapViewModel(
    private val repository: ExchangeRepository = ExchangeRepository.getInstance(),
) : ViewModel() {

    private val _realState = MutableStateFlow<UiState<List<ExchangeMarketInfo>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ExchangeMarketInfo>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val exchanges = repository.getExchanges()
                _realState.value = if (exchanges.isEmpty()) UiState.Empty else UiState.Success(exchanges)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }
}
