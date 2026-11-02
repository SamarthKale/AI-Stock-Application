package com.stockpredictor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.stockpredictor.app.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 4dp-based spacing scale shared by every screen/component so padding never drifts. */
object ClaySpacing {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 12.dp
    val Lg: Dp = 16.dp
    val Xl: Dp = 24.dp
    val Xxl: Dp = 32.dp
    val Avatar: Dp = 64.dp
}

/** Elevation presets consumed by [clayShadow]. */
object ClayElevation {
    val Flat: Dp = 0.dp
    val Small: Dp = 4.dp
    val Default: Dp = 6.dp
    val Large: Dp = 10.dp
}

/** Icon sizes shared across clay components so icon scale never drifts screen to screen. */
object ClayIconSize {
    val Small: Dp = 14.dp   // inline icons, e.g. PriceChangeChip's up/down arrow
    val Medium: Dp = 24.dp  // interactive icon buttons, e.g. watchlist reorder arrows
    val Large: Dp = 48.dp   // state illustrations, e.g. Empty/Error/confirmation icons
}

/** One-off component dimensions that don't fit the spacing or icon-size scales. */
object ClayDimens {
    val SpinnerSize: Dp = 20.dp                        // ClayButton loading indicator
    val SpinnerStroke: Dp = 2.dp                        // ClayButton loading indicator stroke
    val ChartHeight: Dp = 140.dp                        // Crypto Detail placeholder price chart
    val WatchlistTileWidth: Dp = 200.dp                 // Home horizontal watchlist card width
    val ConfidenceBarHeight: Dp = 10.dp                 // PredictionConfidenceBar track height
    val OnboardingDotActive: Dp = 10.dp
    val OnboardingDotInactive: Dp = 8.dp
    val UnreadDotSize: Dp = 8.dp                        // Notifications unread indicator dot
    val UnreadDotTopOffset: Dp = 6.dp                   // aligns the dot with the title's cap height
    val ReadRowIndent: Dp = ClaySpacing.Md + UnreadDotSize // aligns read rows with unread rows' dot+gap width
    val IconTextGap: Dp = 2.dp                          // tight gap between an inline icon and its label
    val ChatbotFabSize: Dp = 48.dp                      // global Chatbot FAB (see ChatbotFab / AppNavHost)
    val HeroCardWidth: Dp = 168.dp                      // HeroImageCard in horizontal carousels
    val HeroCardHeight: Dp = 232.dp
    val AvatarSmall: Dp = 36.dp                         // CoinAvatar in list rows
    val AvatarMedium: Dp = 48.dp                        // AvatarRow items, drawer header
    val AvatarLarge: Dp = 96.dp                         // Profile header photo
    val NavPillHeight: Dp = 72.dp                       // PillBottomNav
    val DrawerWidth: Dp = 300.dp
    val OfficeCardWidth: Dp = 280.dp                    // Map's office carousel cards
}

/**
 * Dual-shadow "clay" emboss: a light offset shape drawn top-left and a dark offset shape
 * drawn bottom-right, both peeking out from behind the opaque content on top of them.
 * [pressed] inverts the offsets to simulate an inset "pushed in" look on tap.
 *
 * Uses solid (non-blurred) offset shapes rather than android.graphics.BlurMaskFilter —
 * blur mask filters are unreliable under hardware-accelerated Compose canvases across
 * API levels, whereas drawing a Path via DrawScope.drawPath is a standard, reliably
 * hardware-accelerated Compose API. The effect reads as a soft dual shadow once the
 * opaque surface is layered on top, without that risk.
 */
fun Modifier.clayShadow(
    shape: Shape = ClayShapes.Medium,
    elevation: Dp = ClayElevation.Default,
    pressed: Boolean = false,
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val offsetPx = elevation.toPx() / 2.5f
    val darkOffset = if (pressed) -offsetPx else offsetPx
    val lightOffset = if (pressed) offsetPx else -offsetPx

    translate(left = darkOffset, top = darkOffset) {
        drawPath(path = path, color = ClayColor.ShadowDark)
    }
    translate(left = lightOffset, top = lightOffset) {
        drawPath(path = path, color = ClayColor.ShadowLight)
    }
}

/** Manrope (OFL, bundled variable font in res/font/manrope.ttf) — one file, every weight. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val ClayFontFamily = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

private val ClayTypography = Typography(
    displayMedium = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp),
    displaySmall = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = ClayFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val ClayColorScheme = lightColorScheme(
    primary = ClayColor.AccentPrimary,
    onPrimary = ClayColor.OnDark,
    primaryContainer = ClayColor.Sand,
    onPrimaryContainer = ClayColor.TextPrimary,
    surfaceVariant = ClayColor.Sand,
    onSurfaceVariant = ClayColor.TextSecondary,
    outline = ClayColor.TextSecondary,
    background = ClayColor.Background,
    onBackground = ClayColor.TextPrimary,
    surface = ClayColor.ClayBase,
    onSurface = ClayColor.TextPrimary,
    secondary = ClayColor.TextSecondary,
    onSecondary = ClayColor.ClayBase,
    error = ClayColor.AccentCoral,
    onError = ClayColor.ClayBase,
)

/** Root theme wrapper (v2 warm minimalist) — every screen sits inside exactly one of these, applied once in MainActivity. */
@Composable
fun ClayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClayColorScheme,
        typography = ClayTypography,
        shapes = androidx.compose.material3.Shapes(
            small = ClayShapes.Small,
            medium = ClayShapes.Medium,
            large = ClayShapes.Large,
        ),
        content = content,
    )
}
