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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music.rtsptotablet.data.model.AppSettings
import com.music.rtsptotablet.data.model.BrightnessMode
import com.music.rtsptotablet.data.model.CameraConfig
import com.music.rtsptotablet.data.model.VideoDisplayMode
import org.json.JSONArray
import org.json.JSONObject
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
        val CAMERAS_JSON = stringPreferencesKey("cameras_json")
        val CURRENT_CAMERA_INDEX = intPreferencesKey("current_camera_index")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val ALLOW_SCREEN_OFF = booleanPreferencesKey("allow_screen_off")
        val BRIGHTNESS_MODE = stringPreferencesKey("brightness_mode")
        val CUSTOM_BRIGHTNESS = floatPreferencesKey("custom_brightness")
        val VIDEO_DISPLAY_MODE = stringPreferencesKey("video_display_mode")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val RECONNECT_DELAY_MS = longPreferencesKey("reconnect_delay_ms")
        // Legacy key for migration
        val RTSP_URL_LEGACY = stringPreferencesKey("rtsp_url")
    }

    /**
     * Flow of current app settings.
     * Emits new values whenever settings change.
     * Includes migration from legacy single URL to camera list.
     */
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        // Parse cameras from JSON or migrate from legacy single URL
        val cameras = preferences[PreferencesKeys.CAMERAS_JSON]?.let { json ->
            parseCamerasJson(json)
        } ?: run {
            // Migration: check for legacy single URL
            val legacyUrl = preferences[PreferencesKeys.RTSP_URL_LEGACY]
            if (!legacyUrl.isNullOrBlank()) {
                listOf(CameraConfig(name = "Camera 1", url = legacyUrl))
            } else {
                emptyList()
            }
        }

        AppSettings(
            cameras = cameras,
            currentCameraIndex = preferences[PreferencesKeys.CURRENT_CAMERA_INDEX] ?: 0,
            isMuted = preferences[PreferencesKeys.IS_MUTED] ?: false,
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
     * Parses cameras list from JSON string.
     */
    private fun parseCamerasJson(json: String): List<CameraConfig> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                CameraConfig(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    url = obj.optString("url", ""),
                    displayMode = obj.optString("displayMode", "").let { mode ->
                        try {
                            if (mode.isNotEmpty()) VideoDisplayMode.valueOf(mode) else VideoDisplayMode.FIT
                        } catch (e: Exception) {
                            VideoDisplayMode.FIT
                        }
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Converts cameras list to JSON string.
     */
    private fun camerasToJson(cameras: List<CameraConfig>): String {
        val jsonArray = JSONArray()
        cameras.forEach { camera ->
            val obj = JSONObject().apply {
                put("id", camera.id)
                put("name", camera.name)
                put("url", camera.url)
                put("displayMode", camera.displayMode.name)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * Updates the list of cameras.
     *
     * @param cameras New list of cameras to save
     */
    suspend fun updateCameras(cameras: List<CameraConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERAS_JSON] = camerasToJson(cameras)
        }
    }

    /**
     * Adds a new camera to the list.
     *
     * @param camera Camera configuration to add
     */
    suspend fun addCamera(camera: CameraConfig) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.CAMERAS_JSON]
            val cameras = currentJson?.let { parseCamerasJson(it) }?.toMutableList() ?: mutableListOf()
            cameras.add(camera)
            preferences[PreferencesKeys.CAMERAS_JSON] = camerasToJson(cameras)
        }
    }

    /**
     * Updates a specific camera.
     *
     * @param camera Updated camera configuration (matched by id)
     */
    suspend fun updateCamera(camera: CameraConfig) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.CAMERAS_JSON]
            val cameras = currentJson?.let { parseCamerasJson(it) }?.toMutableList() ?: mutableListOf()
            val index = cameras.indexOfFirst { it.id == camera.id }
            if (index >= 0) {
                cameras[index] = camera
                preferences[PreferencesKeys.CAMERAS_JSON] = camerasToJson(cameras)
            }
        }
    }

    /**
     * Removes a camera from the list.
     *
     * @param cameraId ID of the camera to remove
     */
    suspend fun removeCamera(cameraId: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.CAMERAS_JSON]
            val cameras = currentJson?.let { parseCamerasJson(it) }?.toMutableList() ?: mutableListOf()
            cameras.removeAll { it.id == cameraId }
            preferences[PreferencesKeys.CAMERAS_JSON] = camerasToJson(cameras)
            // Adjust current index if needed
            val currentIndex = preferences[PreferencesKeys.CURRENT_CAMERA_INDEX] ?: 0
            if (currentIndex >= cameras.size && cameras.isNotEmpty()) {
                preferences[PreferencesKeys.CURRENT_CAMERA_INDEX] = cameras.size - 1
            }
        }
    }

    /**
     * Updates the currently selected camera index.
     *
     * @param index Index of the camera to select
     */
    suspend fun updateCurrentCameraIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_CAMERA_INDEX] = index
        }
    }

    /**
     * Updates the mute state.
     *
     * @param isMuted Whether audio is muted
     */
    suspend fun updateMuteState(isMuted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MUTED] = isMuted
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
            preferences[PreferencesKeys.CAMERAS_JSON] = camerasToJson(settings.cameras)
            preferences[PreferencesKeys.CURRENT_CAMERA_INDEX] = settings.currentCameraIndex
            preferences[PreferencesKeys.IS_MUTED] = settings.isMuted
            preferences[PreferencesKeys.ALLOW_SCREEN_OFF] = settings.allowScreenOff
            preferences[PreferencesKeys.BRIGHTNESS_MODE] = settings.brightnessMode.name
            preferences[PreferencesKeys.CUSTOM_BRIGHTNESS] = settings.customBrightness
            preferences[PreferencesKeys.VIDEO_DISPLAY_MODE] = settings.videoDisplayMode.name
            preferences[PreferencesKeys.AUTO_RECONNECT] = settings.autoReconnect
            preferences[PreferencesKeys.RECONNECT_DELAY_MS] = settings.reconnectDelayMs
        }
    }
}
