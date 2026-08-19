package com.stockpredictor.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

/**
 * Lightweight coin row for contexts with no price data — CoinGecko's /search and
 * /search/trending only return id/symbol/name/image/rank, unlike /coins/markets. Shared by
 * Search results and Home's Trending row rather than tying either to a specific model type.
 */
@Composable
fun CoinRankTile(
    symbol: String,
    name: String,
    marketCapRank: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClayCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        padding = ClaySpacing.Md,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(text = name, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (marketCapRank != null) {
                Text(text = "#$marketCapRank", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
