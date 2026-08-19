package com.stockpredictor.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

@Composable
fun CoinListTile(
    coin: Coin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    ClayCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        padding = ClaySpacing.Md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coin.symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(text = coin.name, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", coin.currentPrice)}",
                    color = ClayColor.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                PriceChangeChip(changePercent = coin.priceChangePercentage24h)
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                trailing()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun CoinListTilePreview() {
    ClayTheme {
        CoinListTile(
            coin = Coin(
                id = "bitcoin",
                symbol = "BTC",
                name = "Bitcoin",
                image = null,
                currentPrice = 62384.50,
                marketCap = null,
                marketCapRank = 1,
                totalVolume = null,
                high24h = null,
                low24h = null,
                priceChange24h = 912.30,
                priceChangePercentage24h = 1.48,
                circulatingSupply = null,
                totalSupply = null,
                maxSupply = null,
                ath = null,
                athChangePercentage = null,
                atl = null,
                atlChangePercentage = null,
                history = listOf(PricePoint(0L, 61472.20)),
                lastUpdated = 0L,
            ),
            onClick = {},
        )
    }
}
