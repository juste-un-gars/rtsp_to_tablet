/**
 * @file PreferencesRepository.kt
 * @description Repository for managing app preferences using DataStore
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music.rtsptotablet.data.model.AppSettings
import com.music.rtsptotablet.data.model.BrightnessMode
import com.music.rtsptotablet.data.model.VideoDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore instance for app preferences */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing application preferences.
 * Uses DataStore for persistent storage of settings.
 *
 * @property context Application context for DataStore access
 *
 * @sample
 * val repository = PreferencesRepository(context)
 * repository.settings.collect { settings ->
 *     // Use settings
 * }
 * repository.updateRtspUrl("rtsp://192.168.1.100:554/stream")
 */
class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val RTSP_URL = stringPreferencesKey("rtsp_url")
        val ALLOW_SCREEN_OFF = booleanPreferencesKey("allow_screen_off")
        val BRIGHTNESS_MODE = stringPreferencesKey("brightness_mode")
        val CUSTOM_BRIGHTNESS = floatPreferencesKey("custom_brightness")
        val VIDEO_DISPLAY_MODE = stringPreferencesKey("video_display_mode")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val RECONNECT_DELAY_MS = longPreferencesKey("reconnect_delay_ms")
    }

    /**
     * Flow of current app settings.
     * Emits new values whenever settings change.
     */
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            rtspUrl = preferences[PreferencesKeys.RTSP_URL] ?: "",
            allowScreenOff = preferences[PreferencesKeys.ALLOW_SCREEN_OFF] ?: true,
            brightnessMode = preferences[PreferencesKeys.BRIGHTNESS_MODE]?.let {
                BrightnessMode.valueOf(it)
            } ?: BrightnessMode.AUTO,
            customBrightness = preferences[PreferencesKeys.CUSTOM_BRIGHTNESS] ?: 0.5f,
            videoDisplayMode = preferences[PreferencesKeys.VIDEO_DISPLAY_MODE]?.let {
                VideoDisplayMode.valueOf(it)
            } ?: VideoDisplayMode.FIT,
            autoReconnect = preferences[PreferencesKeys.AUTO_RECONNECT] ?: true,
            reconnectDelayMs = preferences[PreferencesKeys.RECONNECT_DELAY_MS] ?: 3000L
        )
    }

    /**
     * Updates the RTSP stream URL.
     *
     * @param url New RTSP URL to save
     */
    suspend fun updateRtspUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RTSP_URL] = url
        }
    }

    /**
     * Updates the screen off permission setting.
     *
     * @param allow Whether to allow screen to turn off during playback
     */
    suspend fun updateAllowScreenOff(allow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOW_SCREEN_OFF] = allow
        }
    }

    /**
     * Updates the brightness mode setting.
     *
     * @param mode New brightness mode
     */
    suspend fun updateBrightnessMode(mode: BrightnessMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BRIGHTNESS_MODE] = mode.name
        }
    }

    /**
     * Updates the custom brightness level.
     *
     * @param brightness Brightness value between 0.0 and 1.0
     */
    suspend fun updateCustomBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_BRIGHTNESS] = brightness.coerceIn(0f, 1f)
        }
    }

    /**
     * Updates the video display mode.
     *
     * @param mode New display mode (FIT, FILL, or CROP)
     */
    suspend fun updateVideoDisplayMode(mode: VideoDisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_DISPLAY_MODE] = mode.name
        }
    }

    /**
     * Updates the auto-reconnect setting.
     *
     * @param autoReconnect Whether to automatically reconnect on failure
     */
    suspend fun updateAutoReconnect(autoReconnect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_RECONNECT] = autoReconnect
        }
    }

    /**
     * Updates the reconnection delay.
     *
     * @param delayMs Delay in milliseconds before reconnection attempt
     */
    suspend fun updateReconnectDelay(delayMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECONNECT_DELAY_MS] = delayMs
        }
    }

    /**
     * Updates all settings at once.
     *
     * @param settings New settings to save
     */
    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RTSP_URL] = settings.rtspUrl
            preferences[PreferencesKeys.ALLOW_SCREEN_OFF] = settings.allowScreenOff
            preferences[PreferencesKeys.BRIGHTNESS_MODE] = settings.brightnessMode.name
            preferences[PreferencesKeys.CUSTOM_BRIGHTNESS] = settings.customBrightness
            preferences[PreferencesKeys.VIDEO_DISPLAY_MODE] = settings.videoDisplayMode.name
            preferences[PreferencesKeys.AUTO_RECONNECT] = settings.autoReconnect
            preferences[PreferencesKeys.RECONNECT_DELAY_MS] = settings.reconnectDelayMs
        }
    }
}
