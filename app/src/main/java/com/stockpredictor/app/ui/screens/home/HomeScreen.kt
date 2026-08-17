package com.stockpredictor.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.StockListTile
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // See WatchlistScreen's identical LaunchedEffect for why: the ViewModel survives tab
    // switches but this composable doesn't, so re-query on each entry to this tab.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        // Notifications has no bottom-nav tab (Task 6), so Home surfaces it here;
        // Settings already has its own tab, so it doesn't need a second entry point.
        ClayAppBar(
            title = "AI Stock Predictor",
            trailingIcon = Icons.Filled.Notifications,
            onTrailingClick = onNotificationsClick,
        )
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(
                message = "Your watchlist is empty. Add stocks from Search to see them here.",
                modifier = Modifier.weight(1f),
            )
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> HomeContent(
                data = s.data,
                onStockClick = onStockClick,
                onSearchClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeUiData,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ClaySpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Lg),
    ) {
        item {
            ClayCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onSearchClick)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ClayColor.TextSecondary)
                    Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                    Text("Search stocks…", color = ClayColor.TextSecondary)
                }
            }
        }
        item {
            Text("Your Watchlist", style = MaterialTheme.typography.titleMedium, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
        }
        if (data.watchlistStocks.isEmpty()) {
            item { Text("No stocks in your watchlist yet.", color = ClayColor.TextSecondary) }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                    items(data.watchlistStocks, key = { it.symbol }) { stock ->
                        Box(modifier = Modifier.width(ClayDimens.WatchlistTileWidth)) {
                            StockListTile(stock = stock, onClick = { onStockClick(stock.symbol) })
                        }
                    }
                }
            }
        }
        item {
            Text("Top Movers", style = MaterialTheme.typography.titleMedium, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
        }
        items(moversList(data), key = { it.symbol + "_mover" }) { stock ->
            StockListTile(stock = stock, onClick = { onStockClick(stock.symbol) })
        }
    }
}

private fun moversList(data: HomeUiData): List<Stock> = data.topGainers + data.topLosers
