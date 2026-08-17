package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

/** [confidence] is 0-100. Color ramp: coral <40, amber 40-70, mint >70. */
@Composable
fun PredictionConfidenceBar(
    confidence: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = confidence.coerceIn(0f, 100f)
    val color = when {
        clamped < 40f -> ClayColor.AccentCoral
        clamped <= 70f -> ClayColor.ConfidenceMid
        else -> ClayColor.AccentMint
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ClayDimens.ConfidenceBarHeight)
                .background(ClayColor.TextSecondary.copy(alpha = 0.15f), ClayShapes.Bar),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped / 100f)
                    .fillMaxHeight()
                    .background(color, ClayShapes.Bar),
            )
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Xs))
        Text(
            text = "${clamped.toInt()}% confidence",
            color = ClayColor.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun PredictionConfidenceBarPreview() {
    ClayTheme {
        Column {
            PredictionConfidenceBar(confidence = 25f)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            PredictionConfidenceBar(confidence = 55f)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            PredictionConfidenceBar(confidence = 88f)
        }
    }
}
