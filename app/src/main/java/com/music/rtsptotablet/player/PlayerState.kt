/**
 * @file PlayerState.kt
 * @description Player state definitions and events
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.player

/**
 * Represents the current state of the RTSP player.
 */
sealed class PlayerState {

    /**
     * Player is idle, no stream loaded.
     */
    data object Idle : PlayerState()

    /**
     * Player is buffering/loading the stream.
     */
    data object Buffering : PlayerState()

    /**
     * Player is actively playing the stream.
     */
    data object Playing : PlayerState()

    /**
     * Player is paused.
     */
    data object Paused : PlayerState()

    /**
     * Player encountered an error.
     *
     * @property message Error description
     * @property isRecoverable Whether reconnection can be attempted
     */
    data class Error(
        val message: String,
        val isRecoverable: Boolean = true
    ) : PlayerState()

    /**
     * Player is attempting to reconnect after a failure.
     *
     * @property attemptNumber Current reconnection attempt number
     */
    data class Reconnecting(val attemptNumber: Int) : PlayerState()
}

/**
 * UI state for the player screen.
 *
 * @property playerState Current player state
 * @property rtspUrl Current RTSP URL being played
 * @property showControls Whether to show overlay controls
 * @property isFullscreen Whether player is in fullscreen mode
 * @property isMuted Whether audio is muted
 * @property errorMessage Current error message, if any
 */
data class PlayerUiState(
    val playerState: PlayerState = PlayerState.Idle,
    val rtspUrl: String = "",
    val showControls: Boolean = false,
    val isFullscreen: Boolean = true,
    val isMuted: Boolean = false,
    val errorMessage: String? = null
)
