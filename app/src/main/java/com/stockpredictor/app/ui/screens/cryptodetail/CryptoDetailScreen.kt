package com.stockpredictor.app.ui.screens.cryptodetail

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
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.ml.MomentumResult
import com.stockpredictor.app.ml.MomentumTag
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
import com.stockpredictor.app.ui.components.PredictionDisclaimer
import com.stockpredictor.app.ui.components.PriceChangeChip
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun CryptoDetailScreen(
    coinId: String,
    onBack: () -> Unit,
    viewModel: CryptoDetailViewModel = viewModel(
        factory = CryptoDetailViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            coinId = coinId,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = coinId, onBack = onBack)
        when (val s = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.weight(1f))
            is UiState.Empty -> EmptyState(message = "We don't have data for $coinId.", modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.weight(1f))
            is UiState.Success -> CryptoDetailContent(
                data = s.data,
                onToggleWatchlist = viewModel::toggleWatchlist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CryptoDetailContent(
    data: CryptoDetailData,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coin = data.coin

    Column(modifier = modifier.fillMaxSize().padding(ClaySpacing.Lg)) {
        Text(coin.name, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(ClaySpacing.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$${String.format("%.2f", coin.currentPrice)}",
                color = ClayColor.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(ClaySpacing.Sm))
            PriceChangeChip(changePercent = coin.priceChangePercentage24h)
        }
        if (data.isStale) {
            Spacer(modifier = Modifier.height(ClaySpacing.Xs))
            Text(
                "Showing recently cached data — reconnect to refresh.",
                color = ClayColor.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            PriceHistoryChart(history = coin.history, isPositive = coin.priceChangePercentage24h >= 0.0)
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        PredictionSection(prediction = data.prediction)
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        MomentumSection(momentum = data.momentum)
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
                Text("No prediction available for this coin yet.", color = ClayColor.TextSecondary)
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
                Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                PredictionDisclaimer()
            }
        }
    }
}

/**
 * Phase 5b's on-device momentum tag — deliberately shown as its own small card, separate from
 * [PredictionSection], and captioned "On-device (offline)" so it's never mistaken for the Phase 5
 * server AI Prediction above it. Honest caveat text is shown by design, not omitted: validation
 * (ai-service/artifacts/momentum_training_report.json) found only a marginal edge over a naive
 * majority-class baseline, so this reads as a weak, best-effort local signal rather than a
 * confident call — matching the plan's explicit instruction not to present this feature as more
 * useful than it measured out to be.
 */
@Composable
private fun MomentumSection(momentum: MomentumResult?) {
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("On-Device Momentum", color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(ClaySpacing.Xs))
            Text(
                "Computed on this device from cached price history — works offline, separate from the AI Prediction above.",
                color = ClayColor.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            if (momentum == null) {
                Text("Not enough cached history yet for a momentum tag.", color = ClayColor.TextSecondary)
            } else {
                val (label, color) = when (momentum.tag) {
                    MomentumTag.BULLISH -> "Bullish" to ClayColor.AccentMint
                    MomentumTag.BEARISH -> "Bearish" to ClayColor.AccentCoral
                    MomentumTag.NEUTRAL -> "Neutral" to ClayColor.TextSecondary
                }
                Text(
                    "$label · ${String.format("%.0f", momentum.confidence)}% confidence",
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                Text(
                    "Experimental — validation showed only a weak signal beyond a naive baseline. Not investment advice.",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
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
    Canvas(modifier = modifier.fillMaxWidth().height(ClayDimens.ChartHeight)) {
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
