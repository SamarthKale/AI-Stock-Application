package com.stockpredictor.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme
import com.stockpredictor.app.ui.theme.clayShadow

enum class ClayButtonVariant { Primary, Secondary, Text }

@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ClayButtonVariant = ClayButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, label = "clayButtonScale")
    val isInteractive = enabled && !loading

    val backgroundColor = when {
        variant == ClayButtonVariant.Text -> Color.Transparent
        !enabled -> ClayColor.AccentPrimaryDisabled
        variant == ClayButtonVariant.Primary -> ClayColor.AccentPrimary
        else -> ClayColor.ClayBase
    }
    val contentColor = if (variant == ClayButtonVariant.Primary) ClayColor.ClayBase else ClayColor.TextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .let { base -> if (variant == ClayButtonVariant.Text) base else base.clayShadow(shape = ClayShapes.Medium, pressed = pressed) }
            .background(color = backgroundColor, shape = ClayShapes.Medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isInteractive,
                onClick = onClick,
            )
            .padding(horizontal = ClaySpacing.Xl, vertical = ClaySpacing.Md),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, color = contentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ClayButtonPreview() {
    ClayTheme {
        Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md)) {
            ClayButton(text = "Primary", onClick = {})
            ClayButton(text = "Secondary", onClick = {}, variant = ClayButtonVariant.Secondary)
            ClayButton(text = "Text", onClick = {}, variant = ClayButtonVariant.Text)
            ClayButton(text = "Loading", onClick = {}, loading = true)
            ClayButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
