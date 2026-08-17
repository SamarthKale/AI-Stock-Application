package com.stockpredictor.app.ui.screens.stockdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.components.PredictionConfidenceBar
import com.stockpredictor.app.ui.components.PriceChangeChip
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = viewModel(factory = StockDetailViewModelFactory(symbol)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = symbol, onBack = onBack)
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(message = "We don't have data for $symbol.", modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> StockDetailContent(
                data = s.data,
                onToggleWatchlist = viewModel::toggleWatchlist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StockDetailContent(
    data: StockDetailData,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stock = data.stock
    val currencySymbol = if (stock.exchange in setOf("NASDAQ", "NYSE")) "$" else "₹"

    Column(modifier = modifier.fillMaxSize().padding(ClaySpacing.Lg)) {
        Text(stock.name, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(ClaySpacing.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$currencySymbol${String.format("%.2f", stock.price)}",
                color = ClayColor.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(ClaySpacing.Sm))
            PriceChangeChip(changePercent = stock.changePercent)
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            PriceHistoryChart(history = stock.history, isPositive = stock.changePercent >= 0.0)
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        PredictionSection(prediction = data.prediction)
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        ClayButton(
            text = if (data.isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
            onClick = onToggleWatchlist,
            variant = if (data.isInWatchlist) ClayButtonVariant.Secondary else ClayButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PredictionSection(prediction: Prediction?) {
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("AI Prediction", color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            if (prediction == null) {
                // Small, scoped empty state — not the whole screen's ErrorState — since a
                // missing prediction is additive, not load-bearing (mirrors Phase 5's design).
                Text("No prediction available for this stock yet.", color = ClayColor.TextSecondary)
            } else {
                val directionLabel = when (prediction.direction) {
                    PredictionDirection.Up -> "Bullish"
                    PredictionDirection.Down -> "Bearish"
                    PredictionDirection.Flat -> "Flat"
                }
                Text("$directionLabel · ${prediction.horizon} horizon", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                PredictionConfidenceBar(confidence = prediction.confidence)
                if (prediction.targetPrice != null) {
                    Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                    Text("Target: ${String.format("%.2f", prediction.targetPrice)}", color = ClayColor.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryChart(
    history: List<PricePoint>,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = if (isPositive) ClayColor.AccentMint else ClayColor.AccentCoral
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        if (history.size < 2) return@Canvas
        val prices = history.map { it.price }
        val minPrice = prices.min()
        val maxPrice = prices.max()
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (history.size - 1)
        val path = Path()
        history.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height - ((point.price - minPrice) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
    }
}
