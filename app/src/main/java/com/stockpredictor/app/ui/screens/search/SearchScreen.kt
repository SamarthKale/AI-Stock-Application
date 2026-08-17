package com.stockpredictor.app.ui.screens.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.debug.DebugStateController
import com.stockpredictor.app.debug.DebugUiMode
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.StockListTile
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onStockClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val resultsState by viewModel.uiState.collectAsStateWithLifecycle()
    // Bypassing the blank-query "show recent searches" branch only when a debug mode is
    // active keeps the debug toggle verifiable on this screen without a query typed in.
    val debugMode by DebugStateController.mode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Search", onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = ClaySpacing.Lg)) {
            ClayTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = "Search stocks",
            )
        }
        if (query.isBlank() && debugMode == DebugUiMode.NONE) {
            RecentSearchesSection(
                recentSearches = recentSearches,
                onChipClick = { symbol ->
                    viewModel.onQueryChange(symbol)
                    viewModel.onSearchSubmit(symbol)
                },
            )
        } else {
            when (val s = resultsState) {
                is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
                is UiState.Empty -> EmptyState(message = "No matches for \"$query\".", modifier = Modifier.weight(1f))
                is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(ClaySpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
                ) {
                    items(s.data, key = { it.symbol }) { stock ->
                        StockListTile(
                            stock = stock,
                            onClick = {
                                viewModel.onSearchSubmit(stock.symbol)
                                onStockClick(stock.symbol)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onChipClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(ClaySpacing.Lg)) {
        Text("Recent Searches", color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(ClaySpacing.Sm))
        if (recentSearches.isEmpty()) {
            Text("Your recent searches will show up here.", color = ClayColor.TextSecondary)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm)) {
                items(recentSearches) { search ->
                    Row(
                        modifier = Modifier
                            .background(ClayColor.ClayBase, ClayShapes.Pill)
                            .clickable { onChipClick(search) }
                            .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Sm),
                    ) {
                        Text(text = search, color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
