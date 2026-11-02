package com.stockpredictor.app.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.ui.components.AvatarItem
import com.stockpredictor.app.ui.components.AvatarRow
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.CoinAvatar
import com.stockpredictor.app.ui.components.CoinListTile
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.HeroImageCard
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.SectionHeader
import com.stockpredictor.app.ui.profile.UserProfile
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

@Composable
fun HomeScreen(
    profile: UserProfile,
    onMenuClick: () -> Unit,
    onCoinClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOfficesClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // See WatchlistScreen's identical LaunchedEffect for why: the ViewModel survives tab
    // switches but this composable doesn't, so re-query on each entry to this tab.
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Phase 5c: leaving Home mid-briefing must not leave audio playing over the next screen.
    DisposableEffect(Unit) { onDispose { viewModel.stopBriefing() } }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        HomeHeader(
            profile = profile,
            onMenuClick = onMenuClick,
            onNotificationsClick = onNotificationsClick,
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
                onOfficesClick = onOfficesClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Menu button, notifications and avatar on one row; greeting underneath (the reference's
 *  "27°  Hi Vaishali / Welcome to your Smart Home" block). */
@Composable
private fun HomeHeader(
    profile: UserProfile,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(icon = Icons.Filled.Menu, description = "Open menu", onClick = onMenuClick)
            Spacer(Modifier.weight(1f))
            RoundIconButton(icon = Icons.Filled.Notifications, description = "Notifications", onClick = onNotificationsClick)
            Spacer(Modifier.width(ClaySpacing.Sm))
            Box(
                modifier = Modifier.size(ClayDimens.AvatarMedium).clip(CircleShape).background(ClayColor.Sand),
                contentAlignment = Alignment.Center,
            ) {
                val photo = profile.photo
                if (photo != null) {
                    Image(photo.asImageBitmap(), contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = ClayColor.TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(ClaySpacing.Lg))
        Text("Hi ${profile.firstName}", color = ClayColor.TextPrimary, style = MaterialTheme.typography.displaySmall)
        Text("Welcome to your crypto desk", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ClayDimens.AvatarMedium)
            .clayShadow(shape = CircleShape, elevation = ClayElevation.Small)
            .background(ClayColor.ClayBase, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = ClayColor.TextPrimary)
    }
}

@Composable
private fun HomeContent(
    data: HomeUiData,
    onCoinClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPlayBriefing: () -> Unit,
    onOfficesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ClaySpacing.Lg, end = ClaySpacing.Lg, top = ClaySpacing.Sm, bottom = ClaySpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Lg),
    ) {
        item {
            // Watchlist avatars + gold "+" (opens Search) -- the reference's member strip.
            AvatarRow(
                items = data.watchlistCoins.map { AvatarItem(it.id, it.image, it.symbol) },
                onItemClick = { onCoinClick(it.id) },
                onAddClick = onSearchClick,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                // Search entry point
                ClayCard(
                    modifier = Modifier.weight(1f).clickable(onClick = onSearchClick),
                    shape = ClayShapes.Pill,
                    padding = ClaySpacing.Md,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = ClayColor.TextSecondary)
                        Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                        Text("Search coins…", color = ClayColor.TextSecondary)
                    }
                }
                // Phase 5c: TTS read-out of the watchlist/prediction data this screen already
                // loaded -- no new fetch triggered by tapping this.
                Row(
                    modifier = Modifier
                        .clayShadow(shape = ClayShapes.Pill, elevation = ClayElevation.Small)
                        .background(ClayColor.AccentGold, ClayShapes.Pill)
                        .clickable(onClick = onPlayBriefing)
                        .padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ClayColor.OnDark)
                    Text("Briefing", color = ClayColor.OnDark, fontWeight = FontWeight.SemiBold)
                }
            }
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
            item { SectionHeader("Trending", onClick = onSearchClick) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                    items(data.trending, key = { it.id + "_trending" }) { trending ->
                        HeroImageCard(
                            title = trending.name,
                            subtitle = trending.marketCapRank?.let { "${trending.symbol.uppercase()} · Rank #$it" } ?: trending.symbol.uppercase(),
                            onClick = { onCoinClick(trending.id) },
                            artwork = { CoinAvatar(imageUrl = trending.image, symbol = trending.symbol, size = 88.dp) },
                        )
                    }
                }
            }
        }
        item { SectionHeader("Your Watchlist") }
        if (data.watchlistCoins.isEmpty()) {
            item { Text("No coins in your watchlist yet.", color = ClayColor.TextSecondary) }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
                    items(data.watchlistCoins, key = { it.id }) { coin ->
                        Box(modifier = Modifier.width(ClayDimens.WatchlistTileWidth + 40.dp)) {
                            CoinListTile(coin = coin, onClick = { onCoinClick(coin.id) })
                        }
                    }
                }
            }
        }
        item {
            // Visit-us entry point into the offices map.
            ClayCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOfficesClick), shape = ClayShapes.Large) {
                Column {
                    Text("Visit our offices", color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Mumbai · Pune · Nagpur · Nashik — find the nearest one on the map.", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionHeader("Top Movers") }
        items(moversList(data), key = { it.id + "_mover" }) { coin ->
            CoinListTile(coin = coin, onClick = { onCoinClick(coin.id) })
        }
    }
}

/** Gainers then losers, de-duplicated by id: when the whole market moves one direction (e.g. every
 *  coin is red) the same coin can be in both lists, and a duplicate LazyColumn key crashes the screen. */
private fun moversList(data: HomeUiData): List<Coin> = (data.topGainers + data.topLosers).distinctBy { it.id }
