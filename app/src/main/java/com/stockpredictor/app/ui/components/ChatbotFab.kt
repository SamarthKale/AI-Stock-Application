package com.stockpredictor.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayTheme
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Small floating "Ask AI" bubble placed once at the root [androidx.compose.material3.Scaffold]
 * in AppNavHost (its `floatingActionButton` slot) rather than duplicated per-screen, and shown
 * on every authenticated destination except the Chatbot screen itself -- see
 * `Destinations.chatbotBubbleHiddenRoutes`.
 *
 * Purely a navigation shortcut: tapping it pushes the existing [Destinations.Chatbot] route,
 * the same one Settings' "Ask AI" row already navigates to. It never touches ChatbotViewModel,
 * ChatbotRepository, or the backend chat flow directly -- there is exactly one chatbot
 * implementation, this is just a second entry point into it.
 *
 * Follows [ClayButton]'s existing press-scale pattern (97% scale + inset clay shadow on press)
 * so the interaction language matches the rest of the clay component library.
 */
@Composable
fun ChatbotFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, label = "chatbotFabScale")

    Box(
        modifier = modifier
            .size(ClayDimens.ChatbotFabSize)
            .scale(scale)
            .clayShadow(shape = CircleShape, elevation = ClayElevation.Large, pressed = pressed)
            .background(color = ClayColor.AccentPrimary, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = "Ask AI chatbot" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ChatBubble,
            contentDescription = null,
            tint = ClayColor.ClayBase,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ChatbotFabPreview() {
    ClayTheme {
        ChatbotFab(onClick = {})
    }
}
