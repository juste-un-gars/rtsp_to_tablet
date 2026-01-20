/**
 * @file AppSettings.kt
 * @description Data models for application settings
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.data.model

/**
 * Video display mode options for the player.
 */
enum class VideoDisplayMode {
    /** Fit video inside bounds, may show black bars */
    FIT,
    /** Fill entire screen, may crop video */
    FILL,
    /** Crop video to fill screen while maintaining aspect ratio */
    CROP
}

/**
 * Brightness control mode options.
 */
enum class BrightnessMode {
    /** Follow system brightness settings */
    AUTO,
    /** Use minimum visible brightness */
    MINIMUM,
    /** Use custom brightness level */
    CUSTOM
}

/**
 * Application settings data class.
 * Contains all configurable options for the RTSP player.
 *
 * @property rtspUrl The RTSP stream URL to connect to
 * @property allowScreenOff Whether to allow the screen to turn off during playback
 * @property brightnessMode How to handle screen brightness
 * @property customBrightness Custom brightness level (0.0 to 1.0) when brightnessMode is CUSTOM
 * @property videoDisplayMode How to display the video (fit, fill, crop)
 * @property autoReconnect Whether to automatically reconnect on stream failure
 * @property reconnectDelayMs Delay in milliseconds before attempting reconnection
 *
 * @sample
 * val settings = AppSettings(
 *     rtspUrl = "rtsp://192.168.1.100:554/stream",
 *     allowScreenOff = true,
 *     brightnessMode = BrightnessMode.MINIMUM
 * )
 */
data class AppSettings(
    val rtspUrl: String = "",
    val allowScreenOff: Boolean = true,
    val brightnessMode: BrightnessMode = BrightnessMode.AUTO,
    val customBrightness: Float = 0.5f,
    val videoDisplayMode: VideoDisplayMode = VideoDisplayMode.FIT,
    val autoReconnect: Boolean = true,
    val reconnectDelayMs: Long = 3000L
)
