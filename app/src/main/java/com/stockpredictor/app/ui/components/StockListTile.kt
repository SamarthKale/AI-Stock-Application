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
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.Stock
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

@Composable
fun StockListTile(
    stock: Stock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val currencySymbol = if (stock.exchange in setOf("NASDAQ", "NYSE")) "$" else "₹"
    ClayCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        padding = ClaySpacing.Md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stock.symbol, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(text = stock.name, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format("%.2f", stock.price)}",
                    color = ClayColor.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                PriceChangeChip(changePercent = stock.changePercent)
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
private fun StockListTilePreview() {
    ClayTheme {
        StockListTile(
            stock = Stock(
                symbol = "RELIANCE.NS",
                name = "Reliance Industries",
                exchange = "NSE",
                price = 2938.45,
                change = 42.10,
                changePercent = 1.45,
                history = listOf(PricePoint(0L, 2900.0)),
                lastUpdated = 0L,
            ),
            onClick = {},
        )
    }
}
