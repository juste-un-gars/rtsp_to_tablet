/**
 * @file ScreenManager.kt
 * @description Manages screen state, brightness, and immersive mode
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.screen

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.music.rtsptotablet.data.model.BrightnessMode

/**
 * Manages screen-related functionality including:
 * - Immersive full-screen mode
 * - Screen brightness control
 * - Keep screen on/off settings
 *
 * @property activity The activity to manage screen settings for
 *
 * @sample
 * val screenManager = ScreenManager(activity)
 * screenManager.enterImmersiveMode()
 * screenManager.setMinimumBrightness()
 * screenManager.allowScreenOff()
 */
class ScreenManager(private val activity: Activity) {

    private var originalBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

    init {
        // Save original brightness on initialization
        originalBrightness = activity.window.attributes.screenBrightness
    }

    /**
     * Enters immersive full-screen mode.
     * Hides system bars (status bar and navigation bar) for distraction-free viewing.
     */
    fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    /**
     * Exits immersive mode and shows system bars.
     */
    fun exitImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(true)
            activity.window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * Keeps the screen on during playback.
     * Prevents the screen from turning off automatically.
     */
    fun keepScreenOn() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Allows the screen to turn off based on system timeout settings.
     * Clears the keep screen on flag.
     */
    fun allowScreenOff() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Sets screen brightness based on the specified mode.
     *
     * @param mode The brightness mode to apply
     * @param customValue Custom brightness value (0.0-1.0) for CUSTOM mode
     */
    fun setBrightnessMode(mode: BrightnessMode, customValue: Float = 0.5f) {
        when (mode) {
            BrightnessMode.AUTO -> restoreBrightness()
            BrightnessMode.MINIMUM -> setMinimumBrightness()
            BrightnessMode.CUSTOM -> setCustomBrightness(customValue)
        }
    }

    /**
     * Sets screen brightness to minimum visible level.
     * Useful for nighttime viewing to save energy.
     */
    fun setMinimumBrightness() {
        setCustomBrightness(0.01f)
    }

    /**
     * Sets screen brightness to a custom level.
     *
     * @param brightness Value between 0.0 (minimum) and 1.0 (maximum)
     */
    fun setCustomBrightness(brightness: Float) {
        val params = activity.window.attributes
        params.screenBrightness = brightness.coerceIn(0.01f, 1f)
        activity.window.attributes = params
    }

    /**
     * Restores screen brightness to system default.
     * Removes any custom brightness override.
     */
    fun restoreBrightness() {
        val params = activity.window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params
    }

    /**
     * Restores original brightness saved at initialization.
     */
    fun restoreOriginalBrightness() {
        if (originalBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            val params = activity.window.attributes
            params.screenBrightness = originalBrightness
            activity.window.attributes = params
        } else {
            restoreBrightness()
        }
    }

    /**
     * Gets the current screen brightness value.
     *
     * @return Current brightness (0.0-1.0) or -1 if using system default
     */
    fun getCurrentBrightness(): Float {
        return activity.window.attributes.screenBrightness
    }

    /**
     * Cleans up resources and restores default settings.
     * Should be called when the activity is destroyed.
     */
    fun cleanup() {
        restoreBrightness()
        allowScreenOff()
    }
}
