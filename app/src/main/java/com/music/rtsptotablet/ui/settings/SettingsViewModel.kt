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

    private val _urlInputError = MutableStateFlow<String?>(null)
    val urlInputError: StateFlow<String?> = _urlInputError.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.settings.collect { newSettings ->
                _settings.value = newSettings
            }
        }
    }

    /**
     * Updates the RTSP URL setting.
     * Validates the URL format before saving.
     *
     * @param url New RTSP URL
     */
    fun updateRtspUrl(url: String) {
        _urlInputError.value = validateRtspUrl(url)
        if (_urlInputError.value == null) {
            viewModelScope.launch {
                preferencesRepository.updateRtspUrl(url)
            }
        }
    }

    /**
     * Updates the URL without validation (for real-time input).
     *
     * @param url Current URL input
     */
    fun onRtspUrlChanged(url: String) {
        _settings.value = _settings.value.copy(rtspUrl = url)
        _urlInputError.value = null
    }

    /**
     * Saves the current URL with validation.
     */
    fun saveRtspUrl() {
        val url = _settings.value.rtspUrl
        _urlInputError.value = validateRtspUrl(url)
        if (_urlInputError.value == null) {
            viewModelScope.launch {
                preferencesRepository.updateRtspUrl(url)
            }
        }
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
     * Updates the video display mode.
     *
     * @param mode New display mode
     */
    fun updateVideoDisplayMode(mode: VideoDisplayMode) {
        viewModelScope.launch {
            preferencesRepository.updateVideoDisplayMode(mode)
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
