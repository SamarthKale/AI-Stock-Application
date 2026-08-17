package com.stockpredictor.app.ui.screens.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.PortfolioHolding
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.PriceChangeChip
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun PortfolioScreen(
    onStockClick: (String) -> Unit,
    viewModel: PortfolioViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Portfolio")
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(message = "You don't have any holdings yet.", modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> PortfolioContent(
                data = s.data,
                onStockClick = onStockClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PortfolioContent(
    data: PortfolioUiData,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ClaySpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
    ) {
        item {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Total Value", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${String.format("%.2f", data.totalValue)}",
                            color = ClayColor.TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                        PriceChangeChip(changePercent = data.totalGainLossPercent)
                    }
                }
            }
        }
        items(data.holdings, key = { it.symbol }) { holding ->
            HoldingRow(holding = holding, onClick = { onStockClick(holding.symbol) })
        }
    }
}

@Composable
private fun HoldingRow(holding: PortfolioHolding, onClick: () -> Unit) {
    ClayCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(holding.symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${holding.quantity.toInt()} shares @ ₹${String.format("%.2f", holding.avgBuyPrice)}",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.2f", holding.currentValue)}",
                    color = ClayColor.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                PriceChangeChip(changePercent = holding.gainLossPercent)
            }
        }
    }
}
