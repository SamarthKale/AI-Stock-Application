package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayIconSize
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme
import kotlin.math.abs

@Composable
fun PriceChangeChip(
    changePercent: Double,
    modifier: Modifier = Modifier,
) {
    val isPositive = changePercent >= 0.0
    val color = if (isPositive) ClayColor.AccentMint else ClayColor.AccentCoral
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), ClayShapes.Small)
            .padding(horizontal = ClaySpacing.Sm, vertical = ClaySpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = if (isPositive) "Up" else "Down",
            tint = color,
            modifier = Modifier.size(ClayIconSize.Small),
        )
        Spacer(modifier = Modifier.width(ClayDimens.IconTextGap))
        Text(
            text = "${String.format("%.2f", abs(changePercent))}%",
            color = color,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun PriceChangeChipPreview() {
    ClayTheme {
        Row {
            PriceChangeChip(changePercent = 1.45)
            Spacer(modifier = Modifier.width(ClaySpacing.Sm))
            PriceChangeChip(changePercent = -0.77)
        }
    }
}
