package com.stockpredictor.app.ui.screens.exchangemap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.data.repository.ExchangeMarketInfo
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import kotlin.math.roundToInt
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** OpenFreeMap's "Liberty" style -- live-verified current URL from openfreemap.org's own
 *  quick-start guide at implementation time. No API key, no account, no request cap: OpenFreeMap
 *  serves pre-built vector tiles for free (sponsorship-funded), unlike Google Maps' billed,
 *  key-gated tile loads this migration replaces. */
private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Zoom level the camera animates to once a GPS fix resolves -- a fixed, reasonable "city/region"
 *  zoom, not computed from the nearest-exchange distance (no bounding-box camera-fit logic; kept
 *  deliberately simple per Practical 7's "smallest technically correct" scope). */
private const val USER_LOCATION_ZOOM = 5.0

/**
 * Phase 5c's crypto-specific replacement for the stock-era open/closed exchange map: markers are
 * sized/colored by live 24h trading volume (green = higher, red = lower, via a hand-drawn colored
 * dot bitmap -- MapLibre has no direct equivalent of Google Maps'
 * `BitmapDescriptorFactory.defaultMarker(hue)`), not trading-hours status -- crypto trades 24/7,
 * so "open/closed" has no meaning here. Marker positions are each exchange's
 * registered-jurisdiction capital (see [com.stockpredictor.app.data.ExchangeData]'s doc comment
 * for why that's the honest choice, not a claimed physical trading floor).
 *
 * Map engine: **MapLibre Native Android SDK** (`org.maplibre.gl:android-sdk`, live-verified
 * current stable release at implementation time) rendering **OpenFreeMap**'s free-hosted
 * "Liberty" vector style -- replaces the Google Maps SDK, which needed a billed/restricted API
 * key this project never had configured (see HOW_TO_RUN.md's prior note on that gap). Hosted via
 * a plain [AndroidView] wrapping a MapLibre `MapView`, not a Compose-native binding -- MapLibre's
 * own Jetpack Compose interop is a separate, less-mature artifact not evaluated for this
 * migration; `AndroidView` is the stable, dependency-minimal path and hosts `MapView` exactly the
 * way any other classic-View library would be hosted inside Compose.
 *
 * Practical 7 ("network + multimedia + GPS combined"): the trailing "Locate me" action fuses a
 * one-shot foreground GPS fix ([ExchangeMapViewModel.locateNearestExchange]) with the live
 * CoinGecko network data already on this screen (haversine distance to the nearest of the 8
 * already-fetched [ExchangeMarketInfo] entries) and the existing [com.stockpredictor.app.audio.MarketBriefingSpeaker]
 * TTS multimedia channel -- no new screen, no new network call, no continuous/background
 * location tracking.
 */
@Composable
fun ExchangeMapScreen(
    onBack: () -> Unit,
    viewModel: ExchangeMapViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locateState by viewModel.locateState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ExchangeMarketInfo?>(null) }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.locateNearestExchange() else viewModel.onLocationPermissionDenied()
    }

    val requestLocateMe: () -> Unit = {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.locateNearestExchange()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Once a fix resolves to a nearest exchange, auto-open its existing detail sheet -- reuses
    // exactly the same `selected` state a manual marker tap already drives.
    LaunchedEffect(locateState) {
        (locateState as? LocateMeState.Found)?.let { selected = it.exchange }
    }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(
            title = "Exchange Map",
            onBack = onBack,
            trailingIcon = Icons.Filled.MyLocation,
            onTrailingClick = requestLocateMe,
        )
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is UiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
                is UiState.Empty -> EmptyState(message = "No exchange data available.", modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.fillMaxSize())
                is UiState.Success -> {
                    val foundLocation = locateState as? LocateMeState.Found
                    ExchangeMapContent(
                        exchanges = s.data,
                        userLocation = foundLocation?.let { LatLng(it.userLatitude, it.userLongitude) },
                        onMarkerClick = { selected = it },
                    )
                    // Always-visible OSM attribution -- OpenFreeMap's terms require crediting
                    // OpenStreetMap contributors. MapLibre's own built-in attribution control is a
                    // tap-to-expand "i" icon (kept, at its default position), not always-visible
                    // text, so this is a dedicated label built from the existing clay design
                    // tokens to satisfy "always visible" explicitly.
                    Text(
                        text = "© OpenStreetMap contributors",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(ClaySpacing.Sm)
                            .background(ClayColor.ClayBase.copy(alpha = 0.85f), ClayShapes.Pill)
                            .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    LocateMeStatusBanner(
                        state = locateState,
                        onRetry = requestLocateMe,
                        modifier = Modifier.align(Alignment.TopCenter).padding(ClaySpacing.Lg),
                    )
                    selected?.let { info ->
                        ExchangeDetailSheet(
                            info = info,
                            distanceKm = foundLocation?.takeIf { it.exchange == info }?.distanceKm,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(ClaySpacing.Lg),
                        )
                    }
                }
            }
        }
    }
}

/** Small inline status banner for the locate-me flow -- deliberately not a whole-screen
 *  [ErrorState]: a denied/unavailable GPS fix is a secondary, optional feature failing, not the
 *  exchange map itself, so it gets a small dismissible-by-retry card instead of replacing the map. */
@Composable
private fun LocateMeStatusBanner(state: LocateMeState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        is LocateMeState.Locating -> ClayCard(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ClayDimens.SpinnerSize),
                    color = ClayColor.AccentPrimary,
                    strokeWidth = ClayDimens.SpinnerStroke,
                )
                Spacer(modifier = Modifier.width(ClaySpacing.Sm))
                Text("Getting your location…", color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
        is LocateMeState.Denied -> LocateMeErrorBanner(
            message = "Location permission denied. Grant it to find your nearest exchange.",
            onRetry = onRetry,
            modifier = modifier,
        )
        is LocateMeState.Unavailable -> LocateMeErrorBanner(message = state.message, onRetry = onRetry, modifier = modifier)
        LocateMeState.Idle, is LocateMeState.Found -> Unit
    }
}

@Composable
private fun LocateMeErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ClayCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            ClayButton(text = "Retry", onClick = onRetry, variant = ClayButtonVariant.Secondary)
        }
    }
}

@Composable
private fun ExchangeMapContent(
    exchanges: List<ExchangeMarketInfo>,
    userLocation: LatLng?,
    onMarkerClick: (ExchangeMarketInfo) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val maxVolume = exchanges.mapNotNull { it.tradeVolume24hBtc }.maxOrNull() ?: 0.0
    val iconCache = remember { mutableMapOf<Int, Icon>() }
    val userIcon = remember { userLocationIcon(context) }

    val mapView = remember {
        MapLibre.getInstance(context) // idempotent -- safe to call every time this screen is entered
        MapView(context)
    }
    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    // Drive the MapView's own lifecycle from the hosting Compose lifecycle owner (the nav
    // back-stack entry's lifecycle) -- MapView is a classic View with its own GL-surface
    // lifecycle that Compose's AndroidView does not manage automatically. onCreate/onStart/
    // onResume fire once immediately (composition only happens once the screen is already at
    // least STARTED), then the observer mirrors any later pause/resume/stop from backgrounding
    // the app while this screen is visible; onDispose (navigate away) tears the MapView down.
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(Bundle())
        mapView.onStart()
        mapView.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Load the style once per MapView instance; camera position preserved unchanged from the
    // Google Maps implementation (LatLng(15.0, 20.0), a world-overview zoom). `maplibreMap` is
    // only published once the style finishes loading, so marker sync below never races tile load.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(15.0, 20.0))
                .zoom(1.3)
                .build()
            map.setStyle(OPENFREEMAP_STYLE_URL) {
                maplibreMap = map
            }
        }
    }

    // Re-sync markers whenever the live exchange list changes (e.g. a retry/refresh), the style
    // finishes loading for the first time, OR a GPS fix resolves. All three stay in one effect
    // (rather than a second one keyed only on userLocation) because `map.clear()` below would
    // otherwise wipe out whichever marker set the other effect had just drawn -- the 8 volume-
    // colored exchange markers are rebuilt identically to before this feature; the one addition
    // is the distinct "you are here" marker + camera recenter at the end, added only when a fix
    // exists.
    LaunchedEffect(maplibreMap, exchanges, userLocation) {
        val map = maplibreMap ?: return@LaunchedEffect
        map.clear()
        val markerIdToInfo = mutableMapOf<Long, ExchangeMarketInfo>()
        exchanges.forEach { info ->
            val icon = volumeMarkerIcon(context, iconCache, info.tradeVolume24hBtc, maxVolume)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(info.location.latitude, info.location.longitude))
                    .title(info.location.displayName)
                    .snippet("${info.location.city}, ${info.location.country}")
                    .icon(icon)
            )
            markerIdToInfo[marker.id] = info
        }
        map.setOnMarkerClickListener { marker ->
            markerIdToInfo[marker.id]?.let(onMarkerClick)
            true
        }

        if (userLocation != null) {
            map.addMarker(
                MarkerOptions()
                    .position(userLocation)
                    .title("You are here")
                    .icon(userIcon)
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, USER_LOCATION_ZOOM))
        }
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
}

/** Red (hue 0) for lowest volume, green (hue 120) for highest -- a relative scale among the
 *  currently-fetched exchanges, not an absolute one. Logic unchanged from the Google Maps
 *  implementation; only the rendering (a drawn bitmap dot, not a platform default-marker hue)
 *  differs. */
private fun volumeHue(volume: Double?, maxVolume: Double): Float {
    if (volume == null || maxVolume <= 0.0) return 210f // azure -- same fallback hue Google Maps' HUE_AZURE used
    val ratio = (volume / maxVolume).coerceIn(0.0, 1.0)
    return (ratio * 120.0).toFloat()
}

private const val MARKER_DIAMETER_PX = 40

/** Builds (and caches, per rounded-hue bucket) a small filled-circle bitmap icon -- MapLibre's
 *  [IconFactory] takes a plain [Bitmap], unlike Google Maps' built-in hue-tinted default marker,
 *  so the equivalent color-by-volume effect is hand-drawn once per distinct hue and reused. */
private fun volumeMarkerIcon(
    context: Context,
    cache: MutableMap<Int, Icon>,
    volume: Double?,
    maxVolume: Double,
): Icon {
    val hue = volumeHue(volume, maxVolume)
    val bucket = hue.toInt()
    cache[bucket]?.let { return it }

    val color = AndroidColor.HSVToColor(floatArrayOf(hue, 0.85f, 0.9f))
    val bitmap = Bitmap.createBitmap(MARKER_DIAMETER_PX, MARKER_DIAMETER_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = MARKER_DIAMETER_PX / 2f
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL },
    )
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        },
    )
    val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
    cache[bucket] = icon
    return icon
}

private const val USER_MARKER_DIAMETER_PX = 36

/** Distinct "you are here" marker -- deliberately the app's own [ClayColor.AccentPrimary] brand
 *  color (converted to a plain android.graphics int via [toArgb], not a hardcoded hex, per the
 *  design-token rule) rather than anywhere on the volume red-to-green gradient the 8 exchange
 *  markers already use, so it never gets confused with one of them at a glance. */
private fun userLocationIcon(context: Context): Icon {
    val bitmap = Bitmap.createBitmap(USER_MARKER_DIAMETER_PX, USER_MARKER_DIAMETER_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = USER_MARKER_DIAMETER_PX / 2f
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ClayColor.AccentPrimary.toArgb(); style = Paint.Style.FILL },
    )
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        },
    )
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

@Composable
private fun ExchangeDetailSheet(info: ExchangeMarketInfo, distanceKm: Double? = null, modifier: Modifier = Modifier) {
    ClayCard(modifier = modifier) {
        Column {
            Text(info.location.displayName, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(ClaySpacing.Xs))
            Text("${info.location.city}, ${info.location.country}", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            Row {
                Text("24h volume: ", color = ClayColor.TextSecondary)
                Text(
                    text = info.tradeVolume24hBtc?.let { "${String.format("%.0f", it)} BTC" } ?: "Unavailable",
                    color = ClayColor.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row {
                Text("Trust score: ", color = ClayColor.TextSecondary)
                Text(info.trustScore?.toString() ?: "Unavailable", color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
            }
            if (distanceKm != null) {
                Row {
                    Text("Distance: ", color = ClayColor.TextSecondary)
                    Text("${distanceKm.roundToInt()} km away", color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
