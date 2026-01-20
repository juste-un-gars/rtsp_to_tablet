/**
 * @file PlayerViewModel.kt
 * @description ViewModel for managing player state and business logic
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.music.rtsptotablet.data.model.AppSettings
import com.music.rtsptotablet.data.repository.PreferencesRepository
import com.music.rtsptotablet.player.PlayerState
import com.music.rtsptotablet.player.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the player screen.
 * Manages ExoPlayer instance, RTSP streaming, and reconnection logic.
 *
 * @property context Application context for creating ExoPlayer
 * @property preferencesRepository Repository for accessing app settings
 *
 * @sample
 * val viewModel = PlayerViewModel(context, preferencesRepository)
 * viewModel.startPlayback()
 * viewModel.stopPlayback()
 */
class PlayerViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val newState = when (playbackState) {
                Player.STATE_IDLE -> PlayerState.Idle
                Player.STATE_BUFFERING -> PlayerState.Buffering
                Player.STATE_READY -> if (exoPlayer?.isPlaying == true) PlayerState.Playing else PlayerState.Paused
                Player.STATE_ENDED -> PlayerState.Idle
                else -> PlayerState.Idle
            }
            _uiState.update { it.copy(playerState = newState, errorMessage = null) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                reconnectAttempt = 0
                _uiState.update { it.copy(playerState = PlayerState.Playing, errorMessage = null) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                    "Network connection failed"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Connection timeout"
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                    "Unsupported stream format"
                else -> error.message ?: "Playback error"
            }

            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(errorMessage, isRecoverable = true),
                    errorMessage = errorMessage
                )
            }

            if (_settings.value.autoReconnect) {
                scheduleReconnect()
            }
        }
    }

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { newSettings ->
                _settings.value = newSettings
                _uiState.update { it.copy(rtspUrl = newSettings.rtspUrl) }
            }
        }
    }

    /**
     * Returns the ExoPlayer instance, creating it if necessary.
     *
     * @return ExoPlayer instance
     */
    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(playerListener)
                playWhenReady = true
            }
    }

    /**
     * Starts playback of the configured RTSP stream.
     * Uses the URL from current settings.
     */
    fun startPlayback() {
        val url = _settings.value.rtspUrl
        if (url.isBlank()) {
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error("No RTSP URL configured", isRecoverable = false),
                    errorMessage = "Please configure an RTSP URL in settings"
                )
            }
            return
        }
        startPlayback(url)
    }

    /**
     * Starts playback of a specific RTSP stream.
     *
     * @param rtspUrl The RTSP URL to play
     */
    fun startPlayback(rtspUrl: String) {
        if (rtspUrl.isBlank()) return

        reconnectJob?.cancel()
        reconnectAttempt = 0

        _uiState.update {
            it.copy(
                playerState = PlayerState.Buffering,
                rtspUrl = rtspUrl,
                errorMessage = null
            )
        }

        val player = getPlayer()
        val mediaItem = MediaItem.fromUri(rtspUrl)
        val rtspMediaSource = RtspMediaSource.Factory()
            .createMediaSource(mediaItem)

        player.setMediaSource(rtspMediaSource)
        player.prepare()
    }

    /**
     * Stops playback and releases resources.
     */
    fun stopPlayback() {
        reconnectJob?.cancel()
        exoPlayer?.stop()
        _uiState.update { it.copy(playerState = PlayerState.Idle) }
    }

    /**
     * Pauses playback.
     */
    fun pause() {
        exoPlayer?.pause()
        _uiState.update { it.copy(playerState = PlayerState.Paused) }
    }

    /**
     * Resumes playback.
     */
    fun resume() {
        exoPlayer?.play()
    }

    /**
     * Toggles visibility of overlay controls.
     */
    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    /**
     * Shows overlay controls.
     */
    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
    }

    /**
     * Hides overlay controls.
     */
    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    /**
     * Toggles audio mute state.
     */
    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        exoPlayer?.volume = if (newMuted) 0f else 1f
        _uiState.update { it.copy(isMuted = newMuted) }
    }

    /**
     * Manually triggers a reconnection attempt.
     */
    fun reconnect() {
        reconnectAttempt = 0
        startPlayback()
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            reconnectAttempt++
            _uiState.update {
                it.copy(playerState = PlayerState.Reconnecting(reconnectAttempt))
            }

            delay(_settings.value.reconnectDelayMs)

            if (reconnectAttempt <= MAX_RECONNECT_ATTEMPTS) {
                startPlayback()
            } else {
                _uiState.update {
                    it.copy(
                        playerState = PlayerState.Error(
                            "Max reconnection attempts reached",
                            isRecoverable = true
                        ),
                        errorMessage = "Could not reconnect after $MAX_RECONNECT_ATTEMPTS attempts"
                    )
                }
            }
        }
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }
}
