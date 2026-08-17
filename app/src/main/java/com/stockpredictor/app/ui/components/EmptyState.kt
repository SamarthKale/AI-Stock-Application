package com.stockpredictor.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ClaySpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Filled.Inbox, contentDescription = null, tint = ClayColor.TextSecondary, modifier = Modifier.height(48.dp))
        Spacer(modifier = Modifier.height(ClaySpacing.Md))
        Text(text = message, color = ClayColor.TextSecondary)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(ClaySpacing.Lg))
            ClayButton(text = actionLabel, onClick = onAction, variant = ClayButtonVariant.Secondary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun EmptyStatePreview() {
    ClayTheme { EmptyState(message = "Nothing here yet.", actionLabel = "Retry", onAction = {}) }
}
