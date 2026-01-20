/**
 * @file MainActivity.kt
 * @description Main entry point for the RTSP to Tablet application
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.music.rtsptotablet.data.repository.PreferencesRepository
import com.music.rtsptotablet.navigation.Screen
import com.music.rtsptotablet.screen.ScreenManager
import com.music.rtsptotablet.ui.player.PlayerScreen
import com.music.rtsptotablet.ui.settings.SettingsScreen
import com.music.rtsptotablet.ui.theme.RtspToTabletTheme

/**
 * Main activity that hosts the Compose navigation and manages the app lifecycle.
 * Handles edge-to-edge display and screen management.
 */
class MainActivity : ComponentActivity() {

    private lateinit var screenManager: ScreenManager
    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenManager = ScreenManager(this)
        preferencesRepository = PreferencesRepository(this)

        enableEdgeToEdge()

        setContent {
            RtspToTabletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RtspToTabletNavHost(
                        screenManager = screenManager,
                        preferencesRepository = preferencesRepository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore screen settings when app comes to foreground
        screenManager.enterImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenManager.cleanup()
    }
}

/**
 * Main navigation host for the application.
 * Defines navigation graph with Player and Settings destinations.
 *
 * @param navController Navigation controller for managing navigation state
 * @param screenManager Screen manager for controlling display settings
 * @param preferencesRepository Repository for accessing app preferences
 */
@Composable
fun RtspToTabletNavHost(
    navController: NavHostController = rememberNavController(),
    screenManager: ScreenManager,
    preferencesRepository: PreferencesRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Player.route
    ) {
        composable(Screen.Player.route) {
            PlayerScreen(
                screenManager = screenManager,
                preferencesRepository = preferencesRepository,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                preferencesRepository = preferencesRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
