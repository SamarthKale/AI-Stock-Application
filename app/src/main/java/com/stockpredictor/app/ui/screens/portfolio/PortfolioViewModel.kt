package com.stockpredictor.app.ui.screens.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.mock.MockPortfolio
import com.stockpredictor.app.model.PortfolioHolding
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PortfolioUiData(
    val holdings: List<PortfolioHolding>,
    val totalValue: Double,
    val totalGainLossPercent: Double,
)

class PortfolioViewModel : ViewModel() {
    private val _holdings = MutableStateFlow(MockPortfolio.holdings)

    val uiState: StateFlow<UiState<PortfolioUiData>> = debugAwareUiState(
        dataFlow = _holdings.map { holdings ->
            val totalValue = holdings.sumOf { it.currentValue }
            val totalCost = holdings.sumOf { it.avgBuyPrice * it.quantity }
            val totalGainLossPercent = if (totalCost == 0.0) 0.0 else ((totalValue - totalCost) / totalCost) * 100.0
            PortfolioUiData(holdings, totalValue, totalGainLossPercent)
        },
        isEmpty = { it.holdings.isEmpty() },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
}
