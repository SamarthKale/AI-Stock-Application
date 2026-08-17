package com.stockpredictor.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared corner-radius presets (20-28dp per the design system) so components never invent their own. */
object ClayShapes {
    val Small = RoundedCornerShape(20.dp)
    val Medium = RoundedCornerShape(24.dp)
    val Large = RoundedCornerShape(28.dp)

    // Below the 20-28dp surface scale on purpose: these are small in-line accents
    // (progress-bar track/fill, filter/recent-search pill chips), not clay surfaces.
    val Bar = RoundedCornerShape(6.dp)
    val Pill = RoundedCornerShape(50)
}
