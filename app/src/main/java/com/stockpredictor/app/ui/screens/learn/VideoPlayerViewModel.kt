package com.stockpredictor.app.ui.screens.learn

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.stockpredictor.app.data.learn.LearnContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VideoPlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val ended: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
)

private const val SKIP_MS = 10_000L
private const val POLL_MS = 250L

/**
 * Owns the ExoPlayer so playback state (position, play/pause) survives rotation -- the composable
 * only attaches a PlayerView to [player]. ExoPlayer is created and touched only on the main thread
 * (viewModelScope runs on Dispatchers.Main.immediate). Releasing in [onCleared] is what stops audio
 * when the user leaves the screen (the back-stack entry, and with it this ViewModel, is destroyed).
 */
class VideoPlayerViewModel(application: Application, itemId: String) : AndroidViewModel(application) {

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _state = MutableStateFlow(VideoPlayerUiState())
    val state: StateFlow<VideoPlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(error = "This video couldn't be played.", isPlaying = false, isBuffering = false)
        }
    }

    init {
        val item = LearnContent.byId(itemId)
        val raw = item?.rawRes
        if (item == null || raw == null) {
            _state.value = VideoPlayerUiState(error = "Video not found.", isBuffering = false)
        } else {
            _state.value = _state.value.copy(title = item.title)
            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${application.packageName}/$raw")))
            player.prepare()
            // Position ticks only matter while something is visible and playing; the loop is tied
            // to the ViewModel's scope so it dies with the screen.
            viewModelScope.launch {
                while (isActive) {
                    if (player.isPlaying) publish()
                    delay(POLL_MS)
                }
            }
        }
    }

    private fun publish() {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            ended = player.playbackState == Player.STATE_ENDED,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
        )
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        publish()
    }

    fun pause() {
        player.pause()
        publish()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, (player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)))
        publish()
    }

    fun skipBack() = seekTo(player.currentPosition - SKIP_MS)
    fun skipForward() = seekTo(player.currentPosition + SKIP_MS)

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }
}
