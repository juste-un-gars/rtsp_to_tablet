/**
 * @file Screen.kt
 * @description Navigation routes definition for the app
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.navigation

/**
 * Sealed class defining all navigation destinations in the app.
 *
 * @property route The navigation route string identifier
 *
 * @sample
 * navController.navigate(Screen.Player.route)
 */
sealed class Screen(val route: String) {

    /**
     * Player screen - Full-screen RTSP video playback
     */
    data object Player : Screen("player")

    /**
     * Settings screen - App configuration and preferences
     */
    data object Settings : Screen("settings")
}
