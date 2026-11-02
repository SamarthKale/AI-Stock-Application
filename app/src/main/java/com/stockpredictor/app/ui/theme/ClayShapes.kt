package com.stockpredictor.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared corner-radius presets so components never invent their own (v2: slightly rounder). */
object ClayShapes {
    val Small = RoundedCornerShape(22.dp)
    val Medium = RoundedCornerShape(28.dp)
    val Large = RoundedCornerShape(32.dp)
    /** Big photo-style hero cards (HeroImageCard) and the drawer panel. */
    val Hero = RoundedCornerShape(36.dp)

    // Small in-line accents (progress-bar track/fill, filter/recent-search pill chips),
    // not surfaces.
    val Bar = RoundedCornerShape(6.dp)
    val Pill = RoundedCornerShape(50)
}
