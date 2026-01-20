/**
 * @file AppSettings.kt
 * @description Data models for application settings
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.data.model

/**
 * Configuration for a single camera.
 *
 * @property id Unique identifier for the camera
 * @property name Display name for the camera
 * @property url RTSP URL for the camera stream
 * @property displayMode How to display the video (fit, fill, crop)
 */
data class CameraConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val url: String = "",
    val displayMode: VideoDisplayMode = VideoDisplayMode.FIT
)

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
 * @property cameras List of configured cameras
 * @property currentCameraIndex Index of the currently selected camera
 * @property allowScreenOff Whether to allow the screen to turn off during playback
 * @property brightnessMode How to handle screen brightness
 * @property customBrightness Custom brightness level (0.0 to 1.0) when brightnessMode is CUSTOM
 * @property videoDisplayMode How to display the video (fit, fill, crop)
 * @property autoReconnect Whether to automatically reconnect on stream failure
 * @property reconnectDelayMs Delay in milliseconds before attempting reconnection
 *
 * @sample
 * val settings = AppSettings(
 *     cameras = listOf(CameraConfig(name = "Front Door", url = "rtsp://192.168.1.100:554/stream")),
 *     allowScreenOff = true,
 *     brightnessMode = BrightnessMode.MINIMUM
 * )
 */
data class AppSettings(
    val cameras: List<CameraConfig> = emptyList(),
    val currentCameraIndex: Int = 0,
    val isMuted: Boolean = false,
    val allowScreenOff: Boolean = true,
    val brightnessMode: BrightnessMode = BrightnessMode.AUTO,
    val customBrightness: Float = 0.5f,
    val videoDisplayMode: VideoDisplayMode = VideoDisplayMode.FIT,
    val autoReconnect: Boolean = true,
    val reconnectDelayMs: Long = 3000L
) {
    /** Returns the currently selected camera, or null if no cameras configured */
    val currentCamera: CameraConfig?
        get() = cameras.getOrNull(currentCameraIndex)

    /** Returns the RTSP URL of the current camera for backward compatibility */
    val rtspUrl: String
        get() = currentCamera?.url ?: ""

    /** Returns true if there are multiple cameras configured */
    val hasMultipleCameras: Boolean
        get() = cameras.size > 1

    /** Returns true if navigation to previous camera is possible */
    val canGoPrevious: Boolean
        get() = cameras.size > 1

    /** Returns true if navigation to next camera is possible */
    val canGoNext: Boolean
        get() = cameras.size > 1
}
