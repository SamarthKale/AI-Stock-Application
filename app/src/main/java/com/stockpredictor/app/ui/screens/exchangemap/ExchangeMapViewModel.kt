package com.stockpredictor.app.ui.screens.exchangemap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.stockpredictor.app.audio.MarketBriefingSpeaker
import com.stockpredictor.app.data.repository.ExchangeMarketInfo
import com.stockpredictor.app.data.repository.ExchangeRepository
import com.stockpredictor.app.data.repository.toUserMessage
import com.stockpredictor.app.ui.state.UiState
import com.stockpredictor.app.ui.state.debugAwareUiState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** State for the Exchange Map's Practical-7 "Locate me" action -- entirely separate from
 *  [ExchangeMapViewModel.uiState] (the exchange-list Loading/Empty/Error/Success), since a
 *  location fix is an independent, optional, user-triggered overlay on top of the exchange data,
 *  not a precondition for it. */
sealed interface LocateMeState {
    data object Idle : LocateMeState
    data object Locating : LocateMeState
    data class Found(
        val exchange: ExchangeMarketInfo,
        val distanceKm: Double,
        val userLatitude: Double,
        val userLongitude: Double,
    ) : LocateMeState
    data object Denied : LocateMeState
    data class Unavailable(val message: String) : LocateMeState
}

private const val LOCATION_TIMEOUT_MS = 15_000L
private const val EARTH_RADIUS_KM = 6371.0

/** Great-circle distance between two lat/lon points, in kilometers -- client-side only, no
 *  geocoding/address lookup of any kind (Practical 7's GPS gap is closed with raw coordinates
 *  against the already-known static [com.stockpredictor.app.data.ExchangeData] positions). */
private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}

/**
 * Needs [Application] (unlike a plain [androidx.lifecycle.ViewModel]) for two Practical-7
 * additions: [LocationServices]' [com.google.android.gms.location.FusedLocationProviderClient]
 * and [MarketBriefingSpeaker] (both Context-scoped) -- everything else (the exchange list,
 * [ExchangeRepository]) is unchanged from before this feature. [repository] is intentionally a
 * body-initialized property, not a second defaulted constructor parameter alongside [Application]:
 * Compose's `viewModel()` default factory resolves [AndroidViewModel]s via reflection against an
 * exact `(Application)` constructor, which a defaulted extra parameter would shadow with a
 * different JVM signature and break at runtime.
 */
class ExchangeMapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExchangeRepository = ExchangeRepository.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val briefingSpeaker = MarketBriefingSpeaker(application)

    private val _realState = MutableStateFlow<UiState<List<ExchangeMarketInfo>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ExchangeMarketInfo>>> = debugAwareUiState(_realState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _locateState = MutableStateFlow<LocateMeState>(LocateMeState.Idle)
    val locateState: StateFlow<LocateMeState> = _locateState.asStateFlow()

    private var loadJob: Job? = null
    private var locateJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _realState.value = UiState.Loading
            try {
                val exchanges = repository.getExchanges()
                _realState.value = if (exchanges.isEmpty()) UiState.Empty else UiState.Success(exchanges)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _realState.value = UiState.Error(e.toUserMessage(), ::refresh)
            }
        }
    }

    /** Called by the screen when the runtime permission request comes back denied -- the screen
     *  itself owns the actual system permission dialog (a Compose/Activity-level concern), this
     *  just records the outcome so the small inline retry banner can render. */
    fun onLocationPermissionDenied() {
        _locateState.value = LocateMeState.Denied
    }

    /**
     * One-shot foreground GPS fix (Practical 7). Re-checks permission itself as defense-in-depth
     * (the screen already checks before ever calling this) so a stale/racy caller can never reach
     * [awaitCurrentLocation] without permission and crash with a [SecurityException]. Never
     * registers for continuous updates -- a single [com.google.android.gms.location.FusedLocationProviderClient.getCurrentLocation]
     * call, bounded by [LOCATION_TIMEOUT_MS] so a stuck/no-fix device can't hang this screen.
     */
    fun locateNearestExchange() {
        val app = getApplication<Application>()
        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            _locateState.value = LocateMeState.Denied
            return
        }

        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _locateState.value = LocateMeState.Locating
            try {
                val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { awaitCurrentLocation() }
                if (location == null) {
                    _locateState.value = LocateMeState.Unavailable(
                        "Couldn't get your location. Check GPS is on and try again."
                    )
                    return@launch
                }

                val exchanges = (_realState.value as? UiState.Success)?.data
                if (exchanges.isNullOrEmpty()) {
                    _locateState.value = LocateMeState.Unavailable("Exchange data isn't loaded yet.")
                    return@launch
                }

                val nearest = exchanges.minBy {
                    haversineKm(location.latitude, location.longitude, it.location.latitude, it.location.longitude)
                }
                val distanceKm = haversineKm(
                    location.latitude, location.longitude,
                    nearest.location.latitude, nearest.location.longitude,
                )
                _locateState.value = LocateMeState.Found(nearest, distanceKm, location.latitude, location.longitude)
                briefingSpeaker.speak(buildLocateBriefingText(nearest, distanceKm))
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _locateState.value = LocateMeState.Denied
            } catch (e: Exception) {
                _locateState.value = LocateMeState.Unavailable("Couldn't get your location. Try again.")
            }
        }
    }

    @SuppressLint("MissingPermission") // permission is verified in locateNearestExchange before this is ever called
    private suspend fun awaitCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location -> if (cont.isActive) cont.resumeWith(Result.success(location)) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWith(Result.success(null)) }
            .addOnCanceledListener { if (cont.isActive) cont.resumeWith(Result.success(null)) }
        cont.invokeOnCancellation { cancellationTokenSource.cancel() }
    }

    /** Composed entirely from already-loaded data (the live [ExchangeMarketInfo] the exchange list
     *  already fetched, plus the just-computed haversine distance) -- no new network call, same
     *  "no new network call for the briefing text" discipline [MarketBriefingSpeaker]/
     *  HomeViewModel's briefing already follow. Rounded numbers, simple sentence structure. */
    private fun buildLocateBriefingText(info: ExchangeMarketInfo, distanceKm: Double): String {
        val volumeText = info.tradeVolume24hBtc?.let { "${it.roundToInt()} BTC" } ?: "unavailable"
        return "Nearest exchange: ${info.location.displayName}, ${info.location.city}, " +
            "${distanceKm.roundToInt()} kilometers away. 24 hour volume: $volumeText."
    }

    override fun onCleared() {
        super.onCleared()
        briefingSpeaker.shutdown()
    }
}
