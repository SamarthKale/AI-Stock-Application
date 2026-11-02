package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

data class AvatarItem(val id: String, val imageUrl: String?, val label: String)

/**
 * Pill-shaped strip of circular avatars with a trailing gold "+" -- the reference's family-members
 * row, used on Home for the user's watchlist coins ("+" opens Search to add another).
 */
@Composable
fun AvatarRow(
    items: List<AvatarItem>,
    onItemClick: (AvatarItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .clayShadow(shape = ClayShapes.Pill, elevation = ClayElevation.Small)
            .background(ClayColor.ClayBase, ClayShapes.Pill),
        contentPadding = PaddingValues(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items, key = { it.id }) { item ->
            CoinAvatar(
                imageUrl = item.imageUrl,
                symbol = item.label,
                size = ClayDimens.AvatarMedium,
                modifier = Modifier.clickable { onItemClick(item) },
            )
        }
        item {
            Box(
                modifier = Modifier
                    .size(ClayDimens.AvatarMedium)
                    .background(ClayColor.Sand, CircleShape)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add coin", tint = ClayColor.AccentGold, modifier = Modifier.size(24.dp).padding(0.dp))
            }
        }
    }
}
