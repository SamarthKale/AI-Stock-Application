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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.CoinListTile
import com.stockpredictor.app.ui.components.CoinRankTile
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun HomeScreen(
    onCoinClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // See WatchlistScreen's identical LaunchedEffect for why: the ViewModel survives tab
    // switches but this composable doesn't, so re-query on each entry to this tab.
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Phase 5c: leaving Home mid-briefing must not leave audio playing over the next screen.
    DisposableEffect(Unit) { onDispose { viewModel.stopBriefing() } }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        // Notifications has no bottom-nav tab (Task 6), so Home surfaces it here;
        // Settings already has its own tab, so it doesn't need a second entry point.
        ClayAppBar(
            title = "AI Crypto Predictor",
            trailingIcon = Icons.Filled.Notifications,
            onTrailingClick = onNotificationsClick,
        )
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(
                message = "Your watchlist is empty. Add coins from Search to see them here.",
                modifier = Modifier.weight(1f),
            )
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> HomeContent(
                data = s.data,
                onCoinClick = onCoinClick,
                onSearchClick = onSearchClick,
                onPlayBriefing = viewModel::playBriefing,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeUiData,
    onCoinClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPlayBriefing: () -> Unit,
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
                    Text("Search coins…", color = ClayColor.TextSecondary)
                }
            }
        }
        item {
            // Phase 5c: TTS read-out of the watchlist/prediction data this screen already
            // loaded -- no new fetch triggered by tapping this.
            ClayButton(text = "Play Briefing", onClick = onPlayBriefing, modifier = Modifier.fillMaxWidth())
        }
        if (data.isStale) {
            item {
                Text(
                    "Showing recently cached data — reconnect to refresh.",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (data.trending.isNotEmpty()) {
            item {
                Text("Trending", style = MaterialTheme.typography.titleMedium, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                    items(data.trending, key = { it.id + "_trending" }) { trending ->
                        Box(modifier = Modifier.width(ClayDimens.WatchlistTileWidth)) {
                            CoinRankTile(
                                symbol = trending.symbol,
                                name = trending.name,
                                marketCapRank = trending.marketCapRank,
                                onClick = { onCoinClick(trending.id) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("Your Watchlist", style = MaterialTheme.typography.titleMedium, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
        }
        if (data.watchlistCoins.isEmpty()) {
            item { Text("No coins in your watchlist yet.", color = ClayColor.TextSecondary) }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                    items(data.watchlistCoins, key = { it.id }) { coin ->
                        Box(modifier = Modifier.width(ClayDimens.WatchlistTileWidth)) {
                            CoinListTile(coin = coin, onClick = { onCoinClick(coin.id) })
                        }
                    }
                }
            }
        }
        item {
            Text("Top Movers", style = MaterialTheme.typography.titleMedium, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
        }
        items(moversList(data), key = { it.id + "_mover" }) { coin ->
            CoinListTile(coin = coin, onClick = { onCoinClick(coin.id) })
        }
    }
}

private fun moversList(data: HomeUiData): List<Coin> = data.topGainers + data.topLosers
