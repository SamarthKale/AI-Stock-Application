package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens

/**
 * Round coin logo loaded with Coil from CoinGecko's image URL ([com.stockpredictor.app.model.Coin.image]).
 * While loading, on failure, or when [imageUrl] is null/blank (offline, cache miss), it falls back to
 * a sand-colored circle with the ticker's first letters, so a list never shows a broken image.
 */
@Composable
fun CoinAvatar(
    imageUrl: String?,
    symbol: String,
    modifier: Modifier = Modifier,
    size: Dp = ClayDimens.AvatarSmall,
) {
    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(size).background(ClayColor.Sand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol.take(2).uppercase(),
                color = ClayColor.AccentPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    Box(modifier = modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = symbol,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { fallback() },
                error = { fallback() },
            )
        }
    }
}
