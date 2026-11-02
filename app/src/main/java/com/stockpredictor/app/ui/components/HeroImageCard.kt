package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Tall rounded "photo" card with a bottom gradient scrim and an overlaid title -- the signature
 * surface of the v2 design (the reference's "Living Room"/"Kitchen" tiles). The backdrop is a
 * warm brown gradient (this app has no photography), optionally carrying a large [artwork] slot
 * (a coin logo, an icon, a badge) so each card is still visually distinct.
 */
@Composable
fun HeroImageCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backdropTop: Color = ClayColor.AccentGold,
    backdropBottom: Color = ClayColor.AccentPrimary,
    onClick: (() -> Unit)? = null,
    artwork: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(width = ClayDimens.HeroCardWidth, height = ClayDimens.HeroCardHeight)
            .clayShadow(shape = ClayShapes.Hero, elevation = ClayElevation.Default)
            .background(Brush.verticalGradient(listOf(backdropTop, backdropBottom)), ClayShapes.Hero)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        if (artwork != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 48.dp)) { artwork() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to ClayColor.ScrimTop, 1f to ClayColor.ScrimBottom), ClayShapes.Hero),
        )
        if (badge != null) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(ClaySpacing.Md)) { badge() }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(ClaySpacing.Lg),
        ) {
            Text(
                text = title,
                color = ClayColor.OnDark,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                Text(
                    text = subtitle,
                    color = ClayColor.OnDarkMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFEAE4)
@Composable
private fun HeroImageCardPreview() {
    ClayTheme {
        HeroImageCard(title = "Bitcoin", subtitle = "$62,384  +1.48%", modifier = Modifier.padding(ClaySpacing.Lg))
    }
}
