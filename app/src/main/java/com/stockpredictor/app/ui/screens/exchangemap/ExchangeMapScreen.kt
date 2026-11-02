package com.stockpredictor.app.ui.screens.exchangemap

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.data.OfficeLocation
import com.stockpredictor.app.data.repository.ExchangeMarketInfo
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow
import kotlin.math.roundToInt
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon as MapIcon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** OpenFreeMap's "Liberty" style -- free vector tiles, no API key, no account, no request cap. */
private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Maharashtra-wide view (all four offices fit on a phone-width map at this zoom). */
private val MAHARASHTRA_CENTER = LatLng(19.9, 76.0) // midpoint of the four offices
private const val MAHARASHTRA_ZOOM = 5.0
// Wide enough for every pin in the All view: San Francisco (-122) through the Seychelles (+55)
// to our Indian offices (+73..79).
private val WORLD_CENTER = LatLng(15.0, -22.0)
private const val WORLD_ZOOM = 0.4
private const val OFFICE_FOCUS_ZOOM = 11.0

/**
 * "Offices & Map". The map is always available -- our offices are static data, so the screen no
 * longer waits on (or fails with) the live CoinGecko exchange list the way the original Exchange Map
 * did. Filter chips switch between our [Offices][MapFilter.OFFICES] (default, gold pins), global
 * [Exchanges][MapFilter.EXCHANGES] (volume-coloured dots, live data) or [All][MapFilter.ALL].
 *
 * GPS ("Locate me", the app bar's trailing icon) is the same one-shot foreground fix as before --
 * permission flow, 15 s timeout, no background tracking -- but now always resolves to the nearest
 * *office* (static, so it can't fail on network state) and adds the nearest exchange when that data
 * has loaded. The camera frames you and that office together; per-office distances show in the
 * carousel, and Directions hands off to any installed maps app (falling back to the browser).
 *
 * Engine: MapLibre Native Android + OpenFreeMap, hosted in a plain [AndroidView].
 */
@Composable
fun ExchangeMapScreen(
    onBack: () -> Unit,
    viewModel: ExchangeMapViewModel = viewModel(),
) {
    val exchangeState by viewModel.uiState.collectAsStateWithLifecycle()
    val locateState by viewModel.locateState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val offices = viewModel.offices
    val context = LocalContext.current

    var selectedOffice by remember { mutableStateOf<OfficeLocation?>(null) }
    var selectedExchange by remember { mutableStateOf<ExchangeMarketInfo?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val carouselState = rememberLazyListState()

    val found = locateState as? LocateMeState.Found
    val userLocation = found?.let { LatLng(it.userLatitude, it.userLongitude) }

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

    // A GPS fix selects its nearest office (opens its card) and frames "you + that office".
    LaunchedEffect(locateState) {
        val f = locateState as? LocateMeState.Found ?: return@LaunchedEffect
        selectedExchange = null
        selectedOffice = f.office
        val m = map ?: return@LaunchedEffect
        val user = LatLng(f.userLatitude, f.userLongitude)
        val office = LatLng(f.office.latitude, f.office.longitude)
        val apart = kotlin.math.abs(user.latitude - office.latitude) + kotlin.math.abs(user.longitude - office.longitude)
        if (apart < 0.02) {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(user, 14.0))
        } else {
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().include(user).include(office).build(), 160))
        }
        offices.indexOf(f.office).takeIf { it >= 0 }?.let { carouselState.animateScrollToItem(it) }
    }

    // Filter change -> reframe: Maharashtra for offices, world for exchanges/all.
    LaunchedEffect(filter, map) {
        val m = map ?: return@LaunchedEffect
        if (found != null) return@LaunchedEffect // don't fight a fresh GPS framing
        val (center, zoom) = if (filter == MapFilter.OFFICES) MAHARASHTRA_CENTER to MAHARASHTRA_ZOOM else WORLD_CENTER to WORLD_ZOOM
        m.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
    }

    fun focusOffice(office: OfficeLocation) {
        selectedExchange = null
        selectedOffice = office
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(office.latitude, office.longitude), OFFICE_FOCUS_ZOOM))
    }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(
            title = "Offices & Map",
            onBack = onBack,
            trailingIcon = Icons.Filled.MyLocation,
            onTrailingClick = requestLocateMe,
        )
        Box(modifier = Modifier.weight(1f)) {
            val exchangesVisible = if (filter != MapFilter.OFFICES) (exchangeState as? UiState.Success)?.data.orEmpty() else emptyList()
            val officesVisible = if (filter != MapFilter.EXCHANGES) offices else emptyList()

            OfficeMapContent(
                offices = officesVisible,
                exchanges = exchangesVisible,
                selectedOfficeId = selectedOffice?.id,
                userLocation = userLocation,
                onMapReady = { map = it },
                onOfficeClick = { office ->
                    focusOffice(office)
                },
                onExchangeClick = { selectedOffice = null; selectedExchange = it },
            )

            // ---- top overlay: filter chips + status -------------------------------------------------
            Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(ClaySpacing.Md),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
            ) {
                FilterChips(selected = filter, onSelect = viewModel::setFilter)
                if (filter != MapFilter.OFFICES) ExchangeStatusChip(exchangeState)
                LocateMeStatusBanner(state = locateState, onRetry = requestLocateMe)
            }

            // ---- right-edge map buttons ---------------------------------------------------------------
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = ClaySpacing.Md),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
            ) {
                MapButton(Icons.Filled.Add, "Zoom in") { map?.animateCamera(CameraUpdateFactory.zoomIn()) }
                MapButton(Icons.Filled.Remove, "Zoom out") { map?.animateCamera(CameraUpdateFactory.zoomOut()) }
                MapButton(Icons.Filled.CenterFocusStrong, "Reset view") {
                    val (c, z) = if (filter == MapFilter.OFFICES) MAHARASHTRA_CENTER to MAHARASHTRA_ZOOM else WORLD_CENTER to WORLD_ZOOM
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(c, z))
                }
            }

            // ---- bottom overlay: attribution + office carousel / exchange sheet ------------------------
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
            ) {
                // Always-visible OSM attribution -- OpenFreeMap's terms require crediting OpenStreetMap
                // contributors (MapLibre's own "i" control is tap-to-expand, so it isn't enough).
                Text(
                    text = "© OpenStreetMap contributors",
                    modifier = Modifier
                        .padding(start = ClaySpacing.Sm)
                        .background(ClayColor.ClayBase.copy(alpha = 0.85f), ClayShapes.Pill)
                        .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                val exchange = selectedExchange
                if (exchange != null) {
                    ExchangeDetailSheet(
                        info = exchange,
                        distanceKm = found?.takeIf { it.exchange == exchange }?.exchangeDistanceKm,
                        onClose = { selectedExchange = null },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Md),
                    )
                } else if (filter != MapFilter.EXCHANGES) {
                    LazyRow(
                        state = carouselState,
                        contentPadding = PaddingValues(horizontal = ClaySpacing.Lg),
                        horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
                        modifier = Modifier.padding(bottom = ClaySpacing.Md),
                    ) {
                        itemsIndexed(offices, key = { _, o -> o.id }) { _, office ->
                            OfficeCard(
                                office = office,
                                selected = office.id == selectedOffice?.id,
                                distanceKm = userLocation?.let {
                                    haversineKm(it.latitude, it.longitude, office.latitude, office.longitude)
                                },
                                isNearest = found?.office?.id == office.id,
                                onClick = { focusOffice(office) },
                                onDirections = { openDirections(context, office) },
                                onCall = { office.phone?.let { openDialer(context, it) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================================================
// Overlay widgets
// ================================================================================================

@Composable
private fun FilterChips(selected: MapFilter, onSelect: (MapFilter) -> Unit) {
    Row(
        modifier = Modifier
            .clayShadow(shape = ClayShapes.Pill, elevation = ClayElevation.Small)
            .background(ClayColor.ClayBase, ClayShapes.Pill)
            .padding(ClaySpacing.Xs),
    ) {
        MapFilter.entries.forEach { f ->
            val isSelected = f == selected
            Text(
                text = when (f) {
                    MapFilter.OFFICES -> "Offices"
                    MapFilter.EXCHANGES -> "Exchanges"
                    MapFilter.ALL -> "All"
                },
                color = if (isSelected) ClayColor.OnDark else ClayColor.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(if (isSelected) ClayColor.AccentPrimary else androidx.compose.ui.graphics.Color.Transparent, ClayShapes.Pill)
                    .clickable { onSelect(f) }
                    .padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm),
            )
        }
    }
}

/** Small inline note about the live exchange data -- the map and offices never depend on it. */
@Composable
private fun ExchangeStatusChip(state: UiState<List<ExchangeMarketInfo>>) {
    val text = when (state) {
        is UiState.Loading -> "Loading live exchange data…"
        is UiState.Error -> "Live exchange data unavailable"
        is UiState.Empty -> "No exchange data"
        is UiState.Success -> return
    }
    Row(
        modifier = Modifier
            .background(ClayColor.ClayBase.copy(alpha = 0.92f), ClayShapes.Pill)
            .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state is UiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ClayColor.AccentPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(ClaySpacing.Sm))
        }
        Text(text, color = ClayColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
        if (state is UiState.Error && state.retry != null) {
            Text(
                "  Retry",
                color = ClayColor.AccentPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { state.retry?.invoke() },
            )
        }
    }
}

@Composable
private fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clayShadow(shape = CircleShape, elevation = ClayElevation.Small)
            .background(ClayColor.ClayBase, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ClayColor.TextPrimary)
    }
}

/** Small inline status for the locate-me flow -- a denied/unavailable GPS fix is an optional
 *  feature failing, not the map itself, so it never replaces the map with an error screen. */
@Composable
private fun LocateMeStatusBanner(state: LocateMeState, onRetry: () -> Unit) {
    when (state) {
        is LocateMeState.Locating -> ClayCard(modifier = Modifier.fillMaxWidth(), padding = ClaySpacing.Md) {
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
        is LocateMeState.Found -> ClayCard(modifier = Modifier.fillMaxWidth(), padding = ClaySpacing.Md) {
            Text(
                "You're ${state.officeDistanceKm.roundToInt()} km from our ${state.office.city} office",
                color = ClayColor.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        is LocateMeState.Denied -> LocateMeErrorBanner(
            message = "Location permission denied. Grant it to find your nearest office.",
            onRetry = onRetry,
        )
        is LocateMeState.Unavailable -> LocateMeErrorBanner(message = state.message, onRetry = onRetry)
        LocateMeState.Idle -> Unit
    }
}

@Composable
private fun LocateMeErrorBanner(message: String, onRetry: () -> Unit) {
    ClayCard(modifier = Modifier.fillMaxWidth(), padding = ClaySpacing.Md) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(ClaySpacing.Sm))
            ClayButton(text = "Retry", onClick = onRetry, variant = ClayButtonVariant.Secondary)
        }
    }
}

@Composable
private fun OfficeCard(
    office: OfficeLocation,
    selected: Boolean,
    isNearest: Boolean,
    distanceKm: Double?,
    onClick: () -> Unit,
    onDirections: () -> Unit,
    onCall: () -> Unit,
) {
    ClayCard(
        modifier = Modifier.width(ClayDimens.OfficeCardWidth).clickable(onClick = onClick),
        shape = ClayShapes.Large,
        padding = ClaySpacing.Lg,
        backgroundColor = if (selected) ClayColor.ClayBase else ClayColor.ClayBase.copy(alpha = 0.96f),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    office.name,
                    color = ClayColor.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isNearest) {
                    Text(
                        "NEAREST",
                        color = ClayColor.OnDark,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(ClayColor.AccentGold, ClayShapes.Pill)
                            .padding(horizontal = ClaySpacing.Sm, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(ClaySpacing.Xs))
            Text(office.address, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Spacer(Modifier.height(ClaySpacing.Xs))
            Text(office.hours, color = ClayColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
            if (distanceKm != null) {
                Text(
                    "${distanceKm.roundToInt()} km from you",
                    color = ClayColor.AccentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(ClaySpacing.Md))
            Row(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm)) {
                ClayButton(text = "Directions", onClick = onDirections, variant = ClayButtonVariant.Gold)
                if (office.phone != null) {
                    ClayButton(text = "Call", onClick = onCall, variant = ClayButtonVariant.Secondary)
                }
            }
        }
    }
}

@Composable
private fun ExchangeDetailSheet(
    info: ExchangeMarketInfo,
    distanceKm: Double?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClayCard(modifier = modifier, shape = ClayShapes.Large) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info.location.displayName,
                    color = ClayColor.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = ClayColor.TextSecondary,
                    modifier = Modifier.size(22.dp).clickable(onClick = onClose),
                )
            }
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

// ================================================================================================
// Map
// ================================================================================================

@Composable
private fun OfficeMapContent(
    offices: List<OfficeLocation>,
    exchanges: List<ExchangeMarketInfo>,
    selectedOfficeId: String?,
    userLocation: LatLng?,
    onMapReady: (MapLibreMap) -> Unit,
    onOfficeClick: (OfficeLocation) -> Unit,
    onExchangeClick: (ExchangeMarketInfo) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val maxVolume = exchanges.mapNotNull { it.tradeVolume24hBtc }.maxOrNull() ?: 0.0
    val exchangeIcons = remember { mutableMapOf<Int, MapIcon>() }
    val userIcon = remember { userLocationIcon(context) }
    val officeIcon = remember { officePinIcon(context, selected = false) }
    val officeSelectedIcon = remember { officePinIcon(context, selected = true) }

    val mapView = remember {
        MapLibre.getInstance(context) // idempotent -- safe to call every time this screen is entered
        MapView(context)
    }
    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    // MapView is a classic View with its own GL-surface lifecycle that Compose's AndroidView does not
    // manage: mirror the hosting lifecycle owner into it, and tear it down on dispose.
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

    // Style loads once; `maplibreMap` is only published after the style is ready so marker sync
    // never races tile loading.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder().target(MAHARASHTRA_CENTER).zoom(MAHARASHTRA_ZOOM).build()
            map.uiSettings.isRotateGesturesEnabled = false
            map.setStyle(OPENFREEMAP_STYLE_URL) {
                maplibreMap = map
                onMapReady(map)
            }
        }
    }

    // One effect owns every marker (map.clear() wipes them all, so they must be rebuilt together).
    LaunchedEffect(maplibreMap, offices, exchanges, selectedOfficeId, userLocation) {
        val map = maplibreMap ?: return@LaunchedEffect
        map.clear()
        val idToOffice = mutableMapOf<Long, OfficeLocation>()
        val idToExchange = mutableMapOf<Long, ExchangeMarketInfo>()

        exchanges.forEach { info ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(info.location.latitude, info.location.longitude))
                    .title(info.location.displayName)
                    .icon(volumeMarkerIcon(context, exchangeIcons, info.tradeVolume24hBtc, maxVolume))
            )
            idToExchange[marker.id] = info
        }
        offices.forEach { office ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(office.latitude, office.longitude))
                    .title(office.name)
                    .icon(if (office.id == selectedOfficeId) officeSelectedIcon else officeIcon)
            )
            idToOffice[marker.id] = office
        }
        if (userLocation != null) {
            map.addMarker(MarkerOptions().position(userLocation).title("You are here").icon(userIcon))
        }
        map.setOnMarkerClickListener { marker ->
            idToOffice[marker.id]?.let(onOfficeClick)
            idToExchange[marker.id]?.let(onExchangeClick)
            true // consume: our own cards/sheets replace MapLibre's default info window
        }
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
}

/** Red (hue 0) for lowest volume, green (hue 120) for highest -- relative among fetched exchanges. */
private fun volumeHue(volume: Double?, maxVolume: Double): Float {
    if (volume == null || maxVolume <= 0.0) return 210f
    val ratio = (volume / maxVolume).coerceIn(0.0, 1.0)
    return (ratio * 120.0).toFloat()
}

private const val MARKER_DIAMETER_PX = 40

private fun volumeMarkerIcon(
    context: Context,
    cache: MutableMap<Int, MapIcon>,
    volume: Double?,
    maxVolume: Double,
): MapIcon {
    val hue = volumeHue(volume, maxVolume)
    val bucket = hue.toInt()
    cache[bucket]?.let { return it }

    val color = AndroidColor.HSVToColor(floatArrayOf(hue, 0.85f, 0.9f))
    val bitmap = Bitmap.createBitmap(MARKER_DIAMETER_PX, MARKER_DIAMETER_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = MARKER_DIAMETER_PX / 2f
    canvas.drawCircle(radius, radius, radius - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL })
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f },
    )
    return IconFactory.getInstance(context).fromBitmap(bitmap).also { cache[bucket] = it }
}

private const val USER_MARKER_DIAMETER_PX = 36

/** "You are here": the brand brown, a round dot -- distinct from the gold teardrop office pins and
 *  the red-to-green exchange dots. */
private fun userLocationIcon(context: Context): MapIcon {
    val bitmap = Bitmap.createBitmap(USER_MARKER_DIAMETER_PX, USER_MARKER_DIAMETER_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = USER_MARKER_DIAMETER_PX / 2f
    canvas.drawCircle(radius, radius, radius - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ClayColor.AccentPrimary.toArgb(); style = Paint.Style.FILL })
    canvas.drawCircle(
        radius, radius, radius - 3f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f },
    )
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

/** Teardrop office pin (gold; brown when selected) with a white ring and centre dot. The tip is the
 *  bitmap's bottom-centre, which is where MapLibre anchors a marker icon on its coordinate. */
private fun officePinIcon(context: Context, selected: Boolean): MapIcon {
    val w = 64
    val h = 88
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = w / 2f
    val cy = 30f
    val r = 26f
    val fill = (if (selected) ClayColor.AccentPrimary else ClayColor.AccentGold).toArgb()
    val pin = Path().apply {
        addCircle(cx, cy, r, Path.Direction.CW)
        moveTo(cx - r * 0.82f, cy + r * 0.58f)
        lineTo(cx, h - 3f)
        lineTo(cx + r * 0.82f, cy + r * 0.58f)
        close()
    }
    canvas.drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill; style = Paint.Style.FILL })
    canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f })
    canvas.drawCircle(cx, cy, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.FILL })
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

// ================================================================================================
// Intents
// ================================================================================================

/** Hands off to any installed maps app via a `geo:` URI; if none handles it, opens Google Maps
 *  directions in the browser. Both are free and need no API key. */
private fun openDirections(context: Context, office: OfficeLocation) {
    val lat = office.latitude
    val lng = office.longitude
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(office.name)})"))
    try {
        context.startActivity(geo)
    } catch (e: ActivityNotFoundException) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")))
        }
    }
}

private fun openDialer(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) }
}
