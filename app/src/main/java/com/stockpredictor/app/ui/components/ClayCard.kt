package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme
import com.stockpredictor.app.ui.theme.clayShadow

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShapes.Medium,
    padding: Dp = ClaySpacing.Lg,
    // Phase 5b: lets chat bubbles (ChatbotScreen) tint the user vs. assistant variant
    // differently while still going through the shared clay shadow/shape/padding — every other
    // existing call site keeps the default ClayBase fill unchanged.
    backgroundColor: Color = ClayColor.ClayBase,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clayShadow(shape = shape)
            .background(color = backgroundColor, shape = shape)
            .padding(padding),
        content = content,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ClayCardPreview() {
    ClayTheme {
        ClayCard(modifier = Modifier.fillMaxWidth().padding(ClaySpacing.Lg)) {
            Text("Clay surface", color = ClayColor.TextPrimary)
        }
    }
}
