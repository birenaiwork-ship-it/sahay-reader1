package com.example.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isReady: Boolean = false,
    val isEnded: Boolean = false,
    val errorMessage: String? = null
)

class AudiobookPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private var onPositionSaved: ((Long) -> Unit)? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition
                        )
                        if (isPlaying) {
                            startProgressTracker()
                        } else {
                            stopProgressTracker()
                            onPositionSaved?.invoke(currentPosition)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _playerState.value = _playerState.value.copy(
                                    isReady = true,
                                    durationMs = duration.coerceAtLeast(0L),
                                    currentPositionMs = currentPosition
                                )
                            }
                            Player.STATE_ENDED -> {
                                _playerState.value = _playerState.value.copy(
                                    isPlaying = false,
                                    isEnded = true,
                                    currentPositionMs = duration
                                )
                                stopProgressTracker()
                                onPositionSaved?.invoke(duration)
                            }
                            Player.STATE_IDLE -> {
                                _playerState.value = _playerState.value.copy(isReady = false)
                            }
                            Player.STATE_BUFFERING -> {}
                        }
                    }
                })
            }
        }
    }

    fun loadAudiobook(
        filePath: String,
        initialPositionMs: Long = 0L,
        onPositionChange: ((Long) -> Unit)? = null
    ) {
        initPlayer()
        this.onPositionSaved = onPositionChange

        val file = File(filePath)
        if (!file.exists()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = "Audio file not found at $filePath"
            )
            return
        }

        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        exoPlayer?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            if (initialPositionMs > 0) {
                player.seekTo(initialPositionMs)
            }
            _playerState.value = AudioPlayerState(
                isReady = true,
                currentPositionMs = initialPositionMs,
                playbackSpeed = player.playbackParameters.speed
            )
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) pause() else play()
        }
    }

    fun stop() {
        exoPlayer?.stop()
        stopProgressTracker()
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            val safePos = positionMs.coerceIn(0L, it.duration.coerceAtLeast(0L))
            it.seekTo(safePos)
            _playerState.value = _playerState.value.copy(currentPositionMs = safePos)
            onPositionSaved?.invoke(safePos)
        }
    }

    fun seekRelative(offsetMs: Long) {
        exoPlayer?.let {
            val target = (it.currentPosition + offsetMs).coerceIn(0L, it.duration.coerceAtLeast(0L))
            it.seekTo(target)
            _playerState.value = _playerState.value.copy(currentPositionMs = target)
            onPositionSaved?.invoke(target)
        }
    }

    fun seekBackward10Seconds() = seekRelative(-10_000L)

    fun seekForward30Seconds() = seekRelative(30_000L)

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.let {
            val safeSpeed = speed.coerceIn(0.5f, 2.0f)
            it.playbackParameters = PlaybackParameters(safeSpeed)
            _playerState.value = _playerState.value.copy(playbackSpeed = safeSpeed)
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = scope.launch {
            while (isActive) {
                exoPlayer?.let {
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = it.currentPosition,
                        durationMs = it.duration.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
    }
}
