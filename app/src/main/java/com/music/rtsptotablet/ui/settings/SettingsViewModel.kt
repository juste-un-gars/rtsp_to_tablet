/**
 * @file SettingsViewModel.kt
 * @description ViewModel for managing settings screen state
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.rtsptotablet.data.model.AppSettings
import com.music.rtsptotablet.data.model.BrightnessMode
import com.music.rtsptotablet.data.model.CameraConfig
import com.music.rtsptotablet.data.model.VideoDisplayMode
import com.music.rtsptotablet.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen.
 * Manages settings state and provides methods for updating preferences.
 *
 * @property preferencesRepository Repository for persisting settings
 *
 * @sample
 * val viewModel = SettingsViewModel(preferencesRepository)
 * viewModel.updateRtspUrl("rtsp://192.168.1.100:554/stream")
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Editing state for cameras (id -> edited camera)
    private val _editingCameras = MutableStateFlow<Map<String, CameraConfig>>(emptyMap())
    val editingCameras: StateFlow<Map<String, CameraConfig>> = _editingCameras.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { newSettings ->
                _settings.value = newSettings
            }
        }
    }

    /**
     * Adds a new camera to the list.
     */
    fun addCamera() {
        val currentCameras = _settings.value.cameras
        val newCamera = CameraConfig(
            name = "Camera ${currentCameras.size + 1}",
            url = ""
        )
        viewModelScope.launch {
            preferencesRepository.addCamera(newCamera)
        }
    }

    /**
     * Removes a camera from the list.
     *
     * @param cameraId ID of the camera to remove
     */
    fun removeCamera(cameraId: String) {
        viewModelScope.launch {
            preferencesRepository.removeCamera(cameraId)
        }
        // Clear editing state for this camera
        _editingCameras.value = _editingCameras.value - cameraId
    }

    /**
     * Updates camera name in editing state (not saved yet).
     *
     * @param cameraId ID of the camera
     * @param name New name
     */
    fun onCameraNameChanged(cameraId: String, name: String) {
        val camera = _settings.value.cameras.find { it.id == cameraId } ?: return
        val editingCamera = _editingCameras.value[cameraId] ?: camera
        _editingCameras.value = _editingCameras.value + (cameraId to editingCamera.copy(name = name))
    }

    /**
     * Updates camera URL in editing state (not saved yet).
     *
     * @param cameraId ID of the camera
     * @param url New URL
     */
    fun onCameraUrlChanged(cameraId: String, url: String) {
        val camera = _settings.value.cameras.find { it.id == cameraId } ?: return
        val editingCamera = _editingCameras.value[cameraId] ?: camera
        _editingCameras.value = _editingCameras.value + (cameraId to editingCamera.copy(url = url))
    }

    /**
     * Updates camera display mode (saved immediately).
     *
     * @param cameraId ID of the camera
     * @param mode New display mode
     */
    fun onCameraDisplayModeChanged(cameraId: String, mode: VideoDisplayMode) {
        val camera = _settings.value.cameras.find { it.id == cameraId } ?: return
        viewModelScope.launch {
            preferencesRepository.updateCamera(camera.copy(displayMode = mode))
        }
    }

    /**
     * Saves all pending camera edits.
     */
    fun saveCameras() {
        val editedCameras = _editingCameras.value
        if (editedCameras.isEmpty()) return

        viewModelScope.launch {
            editedCameras.values.forEach { camera ->
                if (validateRtspUrl(camera.url) == null) {
                    preferencesRepository.updateCamera(camera)
                }
            }
            _editingCameras.value = emptyMap()
        }
    }

    /**
     * Gets the current value for a camera field (editing state or saved state).
     *
     * @param cameraId ID of the camera
     * @return Camera config with current editing values
     */
    fun getCameraEditState(cameraId: String): CameraConfig? {
        return _editingCameras.value[cameraId]
            ?: _settings.value.cameras.find { it.id == cameraId }
    }

    /**
     * Validates a camera URL.
     *
     * @param url URL to validate
     * @return Error message or null if valid
     */
    fun validateCameraUrl(url: String): String? {
        return validateRtspUrl(url)
    }

    /**
     * Updates the allow screen off setting.
     *
     * @param allow Whether to allow screen to turn off
     */
    fun updateAllowScreenOff(allow: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAllowScreenOff(allow)
        }
    }

    /**
     * Updates the brightness mode setting.
     *
     * @param mode New brightness mode
     */
    fun updateBrightnessMode(mode: BrightnessMode) {
        viewModelScope.launch {
            preferencesRepository.updateBrightnessMode(mode)
        }
    }

    /**
     * Updates the custom brightness level.
     *
     * @param brightness Brightness value (0.0-1.0)
     */
    fun updateCustomBrightness(brightness: Float) {
        viewModelScope.launch {
            preferencesRepository.updateCustomBrightness(brightness)
        }
    }

    /**
     * Updates the auto-reconnect setting.
     *
     * @param autoReconnect Whether to auto-reconnect on failure
     */
    fun updateAutoReconnect(autoReconnect: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoReconnect(autoReconnect)
        }
    }

    /**
     * Updates the reconnection delay.
     *
     * @param delayMs Delay in milliseconds
     */
    fun updateReconnectDelay(delayMs: Long) {
        viewModelScope.launch {
            preferencesRepository.updateReconnectDelay(delayMs)
        }
    }

    private fun validateRtspUrl(url: String): String? {
        return when {
            url.isBlank() -> null // Empty is allowed
            !url.startsWith("rtsp://") -> "URL must start with rtsp://"
            url.length < 10 -> "URL is too short"
            else -> null
        }
    }
}
