package com.stockpredictor.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the design system's color tokens (v2: warm minimalist — beige
 * surfaces, deep-brown primary, gold CTA accents). Token *names* are unchanged from the original
 * claymorphism palette so every existing screen picks up the new look without edits.
 * No screen or component may hardcode a hex value — always reference this object.
 */
object ClayColor {
    val Background = Color(0xFFEFEAE4)
    val ClayBase = Color(0xFFFBF8F5)
    val AccentPrimary = Color(0xFF8B6B55)
    val AccentMint = Color(0xFF4E9F7E)
    val AccentCoral = Color(0xFFD2685A)
    val TextPrimary = Color(0xFF2A2420)
    val TextSecondary = Color(0xFF9A8F86)

    // v2 additions
    /** Gold used for the primary call-to-action circle/"+" buttons and brand pins. */
    val AccentGold = Color(0xFFC08A3E)
    /** Deep brown panel used by the navigation drawer and dark hero cards. */
    val DrawerSurface = Color(0xFF8B6B55)
    /** Text/icons on [DrawerSurface], [AccentPrimary] and photo-card scrims. */
    val OnDark = Color(0xFFFFFFFF)
    val OnDarkMuted = Color(0xCCFFFFFF)
    /** Gradient stops for text legibility over photo-style hero cards. */
    val ScrimTop = Color(0x00000000)
    val ScrimBottom = Color(0xB32A1D14)
    /** Slightly darker beige for chips, inputs and inactive controls. */
    val Sand = Color(0xFFE5DDD3)

    // Derived states, kept here so pressed/disabled backgrounds never get a one-off hex.
    val AccentPrimaryPressed = AccentPrimary.copy(alpha = 0.12f)
    val AccentPrimaryDisabled = AccentPrimary.copy(alpha = 0.4f)

    // Amber mid-band for PredictionConfidenceBar's 40-70 range; gains/losses only cover
    // the ends of that spectrum in the original token table, so this fills the gap.
    val ConfidenceMid = Color(0xFFE0A83E)

    val Hairline = Color(0x0A000000) // rgba(0,0,0,0.04) — only used if a border is unavoidable.

    // Soft warm drop shadow (v2 is flatter than the original dual-shadow emboss).
    val ShadowLight = Color(0x99FFFFFF)
    val ShadowDark = Color(0x2E5A4030)
}
