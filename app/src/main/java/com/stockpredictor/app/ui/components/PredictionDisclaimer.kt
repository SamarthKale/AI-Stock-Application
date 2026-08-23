package com.stockpredictor.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stockpredictor.app.ui.theme.ClayColor

/**
 * Phase 6 Definition of Done: persistent, clearly-legible — not a one-time dismissible dialog —
 * wherever a server AI prediction confidence/direction value is shown. Styled with
 * `text-secondary` per the Design System (plain text, not an alarming banner) — this is the same
 * wording [com.stockpredictor.app.ui.screens.cryptodetail.CryptoDetailScreen]'s Phase 5b momentum
 * card already used for its own, separate on-device disclaimer.
 */
@Composable
fun PredictionDisclaimer(modifier: Modifier = Modifier) {
    Text(
        "Predictions are for informational purposes only and are not investment advice.",
        color = ClayColor.TextSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.fillMaxWidth(),
    )
}
