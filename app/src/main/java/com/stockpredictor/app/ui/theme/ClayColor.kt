package com.stockpredictor.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the claymorphism design system's color tokens.
 * No screen or component may hardcode a hex value — always reference this object.
 */
object ClayColor {
    val Background = Color(0xFFF4F3F1)
    val ClayBase = Color(0xFFFFFFFF)
    val AccentPrimary = Color(0xFF6C63FF)
    val AccentMint = Color(0xFF4CAF8C)
    val AccentCoral = Color(0xFFE2685A)
    val TextPrimary = Color(0xFF2B2B2E)
    val TextSecondary = Color(0xFF8A8A8E)

    // Derived states, kept here so pressed/disabled backgrounds never get a one-off hex.
    val AccentPrimaryPressed = AccentPrimary.copy(alpha = 0.12f)
    val AccentPrimaryDisabled = AccentPrimary.copy(alpha = 0.4f)

    // Amber mid-band for PredictionConfidenceBar's 40-70 range; gains/losses only cover
    // the ends of that spectrum in the original token table, so this fills the gap.
    val ConfidenceMid = Color(0xFFE0A83E)

    val Hairline = Color(0x0A000000) // rgba(0,0,0,0.04) — only used if a border is unavoidable.

    // Dual-shadow emboss: soft light top-left + soft dark bottom-right.
    val ShadowLight = Color(0xB3FFFFFF)
    val ShadowDark = Color(0x26000000)
}
