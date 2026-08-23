package com.stockpredictor.app.ui.screens.predictions

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.PredictionDirection
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.PredictionConfidenceBar
import com.stockpredictor.app.ui.components.PredictionDisclaimer
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun PredictionsScreen(
    onCoinClick: (String) -> Unit,
    viewModel: PredictionsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Predictions")
        // A simple filter-chip row (per Task 5's guidance) rather than an app-bar filter
        // icon+dropdown — it makes the active filter visible at a glance.
        FilterChipRow(selected = filter, onSelect = viewModel::setFilter)
        // Phase 6 DoD: persistent (not a dismissible dialog), always on-screen without any tap —
        // placed once here rather than per-row so it can't scroll out of view along with the list.
        PredictionDisclaimer(modifier = Modifier.padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Xs))
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(message = "No predictions match this filter.", modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(ClaySpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
            ) {
                items(s.data, key = { it.coin.id }) { row ->
                    ClayCard(
                        modifier = Modifier.fillMaxWidth().clickable { onCoinClick(row.coin.id) },
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.coin.symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(directionLabel(row.prediction.direction), color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                            PredictionConfidenceBar(confidence = row.prediction.confidence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(selected: PredictionFilter, onSelect: (PredictionFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
    ) {
        items(PredictionFilter.entries.toList()) { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .background(
                        color = if (isSelected) ClayColor.AccentPrimary else ClayColor.ClayBase,
                        shape = ClayShapes.Pill,
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Sm),
            ) {
                Text(
                    text = filterLabel(option),
                    color = if (isSelected) ClayColor.ClayBase else ClayColor.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun filterLabel(filter: PredictionFilter): String = when (filter) {
    PredictionFilter.ALL -> "All"
    PredictionFilter.UP -> "Up"
    PredictionFilter.DOWN -> "Down"
    PredictionFilter.FLAT -> "Flat"
}

private fun directionLabel(direction: PredictionDirection): String = when (direction) {
    PredictionDirection.Up -> "Bullish"
    PredictionDirection.Down -> "Bearish"
    PredictionDirection.Flat -> "Flat"
}
