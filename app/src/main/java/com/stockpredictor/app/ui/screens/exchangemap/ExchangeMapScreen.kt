package com.stockpredictor.app.ui.screens.exchangemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.stockpredictor.app.data.repository.ExchangeMarketInfo
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.components.LoadingState
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

/**
 * Phase 5c's crypto-specific replacement for the stock-era open/closed exchange map: markers are
 * sized/colored by live 24h trading volume (green = higher, red = lower, via
 * [BitmapDescriptorFactory]'s hue parameter), not trading-hours status -- crypto trades 24/7, so
 * "open/closed" has no meaning here. Marker positions are each exchange's registered-jurisdiction
 * capital (see [com.stockpredictor.app.data.ExchangeData]'s doc comment for why that's the honest
 * choice, not a claimed physical trading floor).
 */
@Composable
fun ExchangeMapScreen(
    onBack: () -> Unit,
    viewModel: ExchangeMapViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ExchangeMarketInfo?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Exchange Map", onBack = onBack)
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is UiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
                is UiState.Empty -> EmptyState(message = "No exchange data available.", modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(message = s.message, onRetry = s.retry, modifier = Modifier.fillMaxSize())
                is UiState.Success -> {
                    ExchangeMapContent(
                        exchanges = s.data,
                        onMarkerClick = { selected = it },
                    )
                    selected?.let { info ->
                        ExchangeDetailSheet(
                            info = info,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(ClaySpacing.Lg),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeMapContent(
    exchanges: List<ExchangeMarketInfo>,
    onMarkerClick: (ExchangeMarketInfo) -> Unit,
) {
    val maxVolume = exchanges.mapNotNull { it.tradeVolume24hBtc }.maxOrNull() ?: 0.0
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(15.0, 20.0), 1.3f)
    }
    GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
        exchanges.forEach { info ->
            key(info.location.id) {
                Marker(
                    state = rememberMarkerState(position = LatLng(info.location.latitude, info.location.longitude)),
                    title = info.location.displayName,
                    snippet = "${info.location.city}, ${info.location.country}",
                    icon = BitmapDescriptorFactory.defaultMarker(volumeHue(info.tradeVolume24hBtc, maxVolume)),
                    onClick = { onMarkerClick(info); false },
                )
            }
        }
    }
}

/** Red (hue 0) for lowest volume, green (hue 120) for highest -- a relative scale among the
 *  currently-fetched exchanges, not an absolute one. */
private fun volumeHue(volume: Double?, maxVolume: Double): Float {
    if (volume == null || maxVolume <= 0.0) return BitmapDescriptorFactory.HUE_AZURE
    val ratio = (volume / maxVolume).coerceIn(0.0, 1.0)
    return (ratio * 120.0).toFloat()
}

@Composable
private fun ExchangeDetailSheet(info: ExchangeMarketInfo, modifier: Modifier = Modifier) {
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
        }
    }
}
