/**
 * @file PlayerScreen.kt
 * @description Full-screen player UI with overlay controls
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.music.rtsptotablet.data.model.VideoDisplayMode
import com.music.rtsptotablet.data.repository.PreferencesRepository
import com.music.rtsptotablet.player.PlayerState
import com.music.rtsptotablet.screen.ScreenManager
import kotlinx.coroutines.delay

/**
 * Full-screen player composable with overlay controls.
 * Displays RTSP stream with touch-to-show controls.
 *
 * @param screenManager Screen manager for display settings
 * @param preferencesRepository Repository for app settings
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param modifier Modifier for the composable
 */
@Composable
fun PlayerScreen(
    screenManager: ScreenManager,
    preferencesRepository: PreferencesRepository,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel = remember {
        PlayerViewModel(context, preferencesRepository)
    }

    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Apply screen settings
    LaunchedEffect(settings) {
        if (settings.allowScreenOff) {
            screenManager.allowScreenOff()
        } else {
            screenManager.keepScreenOn()
        }
        screenManager.setBrightnessMode(settings.brightnessMode, settings.customBrightness)
    }

    // Auto-hide controls after delay
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(CONTROLS_HIDE_DELAY_MS)
            viewModel.hideControls()
        }
    }

    // Start playback when URL is available
    LaunchedEffect(settings.rtspUrl) {
        if (settings.rtspUrl.isNotBlank()) {
            viewModel.startPlayback()
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                Lifecycle.Event.ON_DESTROY -> viewModel.stopPlayback()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.toggleControls()
            }
    ) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.getPlayer()
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                // Update resize mode based on current camera's display mode
                playerView.resizeMode = when (settings.currentCamera?.displayMode) {
                    VideoDisplayMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoDisplayMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoDisplayMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    null -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading/buffering indicator
        when (val state = uiState.playerState) {
            is PlayerState.Buffering, is PlayerState.Reconnecting -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        if (state is PlayerState.Reconnecting) {
                            Text(
                                text = "Reconnecting... (${state.attemptNumber})",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            else -> {}
        }

        // Error display
        uiState.errorMessage?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        onClick = { viewModel.reconnect() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Overlay controls
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerOverlayControls(
                isMuted = uiState.isMuted,
                hasMultipleCameras = settings.hasMultipleCameras,
                currentCameraName = settings.currentCamera?.name,
                onCloseClick = { /* Exit app or go back */ },
                onSettingsClick = {
                    viewModel.hideControls()
                    onNavigateToSettings()
                },
                onMuteClick = { viewModel.toggleMute() },
                onPreviousClick = { viewModel.previousCamera() },
                onNextClick = { viewModel.nextCamera() }
            )
        }

        // No camera configured message
        if (settings.cameras.isEmpty() && uiState.playerState is PlayerState.Idle) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No camera configured",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tap to open settings",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Overlay controls displayed on top of the video.
 * Shows close (X), settings (gear), mute, and navigation arrows.
 *
 * @param isMuted Whether audio is currently muted
 * @param hasMultipleCameras Whether there are multiple cameras configured
 * @param currentCameraName Name of the current camera (for display)
 * @param onCloseClick Callback when close button is clicked
 * @param onSettingsClick Callback when settings button is clicked
 * @param onMuteClick Callback when mute button is clicked
 * @param onPreviousClick Callback when previous arrow is clicked
 * @param onNextClick Callback when next arrow is clicked
 */
@Composable
private fun PlayerOverlayControls(
    isMuted: Boolean,
    hasMultipleCameras: Boolean,
    currentCameraName: String?,
    onCloseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMuteClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar with close, camera name, mute, and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button (top left)
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Camera name (center top)
            if (!currentCameraName.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentCameraName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Right side buttons (mute + settings)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mute button
                IconButton(
                    onClick = onMuteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Navigation arrows (center sides) - only show if multiple cameras
        if (hasMultipleCameras) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous camera (left)
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next camera (right)
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private const val CONTROLS_HIDE_DELAY_MS = 3000L
