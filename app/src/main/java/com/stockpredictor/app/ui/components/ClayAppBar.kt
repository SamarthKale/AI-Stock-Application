package com.stockpredictor.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

@Composable
fun ClayAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ClaySpacing.Sm, vertical = ClaySpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ClayColor.TextPrimary)
            }
        } else {
            Spacer(modifier = Modifier.width(ClaySpacing.Xxl))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(horizontal = ClaySpacing.Sm),
            color = ClayColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        if (trailingIcon != null && onTrailingClick != null) {
            IconButton(onClick = onTrailingClick) {
                Icon(imageVector = trailingIcon, contentDescription = null, tint = ClayColor.TextPrimary)
            }
        } else {
            Spacer(modifier = Modifier.width(ClaySpacing.Xxl))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ClayAppBarPreview() {
    ClayTheme {
        ClayAppBar(title = "Crypto Detail", onBack = {}, trailingIcon = Icons.Filled.Notifications, onTrailingClick = {})
    }
}
