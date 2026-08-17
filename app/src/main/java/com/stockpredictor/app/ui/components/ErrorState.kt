package com.stockpredictor.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayIconSize
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ClaySpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null, tint = ClayColor.AccentCoral, modifier = Modifier.height(ClayIconSize.Large))
        Spacer(modifier = Modifier.height(ClaySpacing.Md))
        Text(text = message, color = ClayColor.TextPrimary)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(ClaySpacing.Lg))
            ClayButton(text = "Retry", onClick = onRetry, variant = ClayButtonVariant.Secondary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ErrorStatePreview() {
    ClayTheme { ErrorState(message = "Couldn't load data.", onRetry = {}) }
}
