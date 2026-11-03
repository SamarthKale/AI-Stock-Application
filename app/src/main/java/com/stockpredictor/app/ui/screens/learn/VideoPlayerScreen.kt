package com.stockpredictor.app.ui.screens.learn

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ErrorState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Tutorial/demo video player: ExoPlayer for decoding, our own clay controls (play/pause, +-10s,
 * seek bar with elapsed/total time, fullscreen) instead of Media3's stock controller so it matches
 * the design system. Pauses automatically when the app goes to the background.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(itemId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VideoPlayerViewModel = viewModel(
        key = "video_$itemId",
        factory = viewModelFactory {
            initializer { VideoPlayerViewModel(context.applicationContext as Application, itemId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    // Pause when backgrounded so audio never keeps playing behind another app / the lock screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostActivity = context as? android.app.Activity
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // A rotation (fullscreen toggle) recreates the Activity and fires ON_PAUSE too, but the
            // user hasn't left -- only pause when the app is genuinely going to the background.
            if (event == Lifecycle.Event.ON_PAUSE && hostActivity?.isChangingConfigurations != true) viewModel.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Fullscreen = landscape; always restore the default orientation when leaving the screen.
    val activity = context as? android.app.Activity
    DisposableEffect(fullscreen) {
        activity?.requestedOrientation =
            if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    // Immersive fullscreen: hide the status/navigation bars while fullscreen, restore them afterwards.
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(fullscreen) {
        val window = activity?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        if (fullscreen) {
            controller?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(enabled = fullscreen) { fullscreen = false }

    if (state.error != null) {
        Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
            ClayAppBar(title = state.title.ifBlank { "Video" }, onBack = onBack)
            ErrorState(message = state.error!!, onRetry = null, modifier = Modifier.weight(1f))
        }
        return
    }

    // Fullscreen controls get out of the way: they hide 3 s after playback starts and reappear on a
    // tap (or whenever playback is paused/ended, so a paused video is never left without controls).
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, state.isPlaying, fullscreen) {
        if (fullscreen && controlsVisible && state.isPlaying) {
            delay(3_000)
            controlsVisible = false
        }
    }

    val videoSurface: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .background(Color.Black, if (fullscreen) androidx.compose.ui.graphics.RectangleShape else ClayShapes.Large)
                .clickable { if (fullscreen) controlsVisible = !controlsVisible else viewModel.togglePlayPause() }
                .semantics { contentDescription = if (fullscreen) "Video. Tap to show controls." else "Video. Tap to play or pause." },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(android.graphics.Color.BLACK)
                        player = viewModel.player
                    }
                },
                update = { it.player = viewModel.player },
            )
            if (state.isBuffering) CircularProgressIndicator(color = ClayColor.OnDark)
        }
    }
    val controls: @Composable (Modifier) -> Unit = { modifier ->
        Controls(
            state = state,
            fullscreen = fullscreen,
            onTogglePlay = viewModel::togglePlayPause,
            onSkipBack = viewModel::skipBack,
            onSkipForward = viewModel::skipForward,
            onSeek = viewModel::seekTo,
            onToggleFullscreen = { fullscreen = !fullscreen },
            modifier = modifier,
        )
    }

    if (fullscreen) {
        // Landscape: the video fills the whole screen and the controls float over its bottom edge.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            videoSurface(Modifier.fillMaxSize())
            if (controlsVisible || !state.isPlaying) {
                controls(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.82f)
                        .padding(bottom = ClaySpacing.Md)
                        .background(ClayColor.ClayBase.copy(alpha = 0.94f), ClayShapes.Large),
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
            ClayAppBar(title = state.title.ifBlank { "Video" }, onBack = onBack)
            videoSurface(Modifier.weight(1f).fillMaxWidth().padding(horizontal = ClaySpacing.Lg))
            controls(Modifier)
        }
    }
}

@Composable
private fun Controls(
    state: VideoPlayerUiState,
    fullscreen: Boolean,
    onTogglePlay: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // While the thumb is being dragged show the drag position, not the (still-playing) real one.
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val fraction = scrubFraction ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val shownPosition = scrubFraction?.let { (it * duration).toLong() } ?: state.positionMs

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = ClaySpacing.Lg, vertical = if (fullscreen) ClaySpacing.Sm else ClaySpacing.Md)) {
        Slider(
            value = fraction,
            onValueChange = { scrubFraction = it },
            onValueChangeFinished = {
                scrubFraction?.let { onSeek((it * duration).toLong()) }
                scrubFraction = null
            },
            colors = SliderDefaults.colors(
                thumbColor = ClayColor.AccentGold,
                activeTrackColor = ClayColor.AccentGold,
                inactiveTrackColor = ClayColor.Sand,
            ),
            modifier = Modifier.semantics { contentDescription = "Seek bar" },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shownPosition), color = ClayColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(state.durationMs), color = ClayColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(ClaySpacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(Icons.Filled.Replay10, "Back 10 seconds", onSkipBack)
            // Big play/pause: the main control, so it's the gold primary circle.
            val playIcon = when {
                state.ended -> Icons.Filled.Replay
                state.isPlaying -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            val playLabel = when {
                state.ended -> "Replay"
                state.isPlaying -> "Pause"
                else -> "Play"
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clayShadow(shape = CircleShape, elevation = ClayElevation.Default)
                    .background(ClayColor.AccentGold, CircleShape)
                    .clickable(onClick = onTogglePlay)
                    .semantics { contentDescription = playLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(playIcon, contentDescription = null, tint = ClayColor.OnDark, modifier = Modifier.size(36.dp))
            }
            ControlButton(Icons.Filled.Forward10, "Forward 10 seconds", onSkipForward)
            ControlButton(
                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                if (fullscreen) "Exit fullscreen" else "Fullscreen",
                onToggleFullscreen,
            )
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clayShadow(shape = CircleShape, elevation = ClayElevation.Small)
            .background(ClayColor.ClayBase, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ClayColor.TextPrimary)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
