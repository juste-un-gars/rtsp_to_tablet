/**
 * @file SettingsScreen.kt
 * @description Settings UI for configuring the RTSP player
 * @session SESSION_001
 * @created 2026-01-20
 */

package com.music.rtsptotablet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.music.rtsptotablet.data.model.BrightnessMode
import com.music.rtsptotablet.data.model.CameraConfig
import com.music.rtsptotablet.data.model.VideoDisplayMode
import com.music.rtsptotablet.data.repository.PreferencesRepository

/**
 * Settings screen for configuring the RTSP player.
 * Allows configuration of URL, screen behavior, and display options.
 *
 * @param preferencesRepository Repository for settings access
 * @param onNavigateBack Callback to navigate back to player
 * @param modifier Modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember { SettingsViewModel(preferencesRepository) }
    val settings by viewModel.settings.collectAsState()
    val editingCameras by viewModel.editingCameras.collectAsState()
    val focusManager = LocalFocusManager.current

    // Save cameras when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveCameras()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveCameras()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cameras Section
            SettingsSection(title = "Cameras") {
                // Camera list
                settings.cameras.forEachIndexed { index, camera ->
                    val editingCamera = editingCameras[camera.id] ?: camera
                    CameraConfigCard(
                        camera = editingCamera,
                        index = index + 1,
                        onNameChange = { viewModel.onCameraNameChanged(camera.id, it) },
                        onUrlChange = { viewModel.onCameraUrlChanged(camera.id, it) },
                        onDisplayModeChange = { viewModel.onCameraDisplayModeChanged(camera.id, it) },
                        onDelete = { viewModel.removeCamera(camera.id) },
                        onDone = {
                            viewModel.saveCameras()
                            focusManager.clearFocus()
                        },
                        canDelete = settings.cameras.size > 1
                    )
                    if (index < settings.cameras.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Add camera button
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { viewModel.addCamera() },
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add camera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add camera",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Screen Settings Section
            SettingsSection(title = "Screen Settings") {
                // Allow screen off
                SettingsSwitch(
                    title = "Allow screen to turn off",
                    description = "Screen can turn off during playback to save power",
                    checked = settings.allowScreenOff,
                    onCheckedChange = { viewModel.updateAllowScreenOff(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Brightness mode
                Text(
                    text = "Brightness Mode",
                    style = MaterialTheme.typography.bodyLarge
                )
                Column(modifier = Modifier.selectableGroup()) {
                    BrightnessMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.brightnessMode == mode,
                                    onClick = { viewModel.updateBrightnessMode(mode) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.brightnessMode == mode,
                                onClick = null
                            )
                            Text(
                                text = mode.toDisplayString(),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Custom brightness slider
                if (settings.brightnessMode == BrightnessMode.CUSTOM) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Brightness: ${(settings.customBrightness * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.customBrightness,
                            onValueChange = { viewModel.updateCustomBrightness(it) },
                            valueRange = 0.01f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider()

            // Reconnection Settings Section
            SettingsSection(title = "Reconnection") {
                SettingsSwitch(
                    title = "Auto-reconnect",
                    description = "Automatically reconnect when stream fails",
                    checked = settings.autoReconnect,
                    onCheckedChange = { viewModel.updateAutoReconnect(it) }
                )

                if (settings.autoReconnect) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "Reconnect delay: ${settings.reconnectDelayMs / 1000}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.reconnectDelayMs.toFloat(),
                            onValueChange = { viewModel.updateReconnectDelay(it.toLong()) },
                            valueRange = 1000f..10000f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Section header and content wrapper.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

/**
 * Switch setting with title and description.
 */
@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Card for configuring a single camera.
 */
@Composable
private fun CameraConfigCard(
    camera: CameraConfig,
    index: Int,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDisplayModeChange: (VideoDisplayMode) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    canDelete: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with index and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera $index",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete camera",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = camera.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                placeholder = { Text("e.g. Front Door") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // URL field
            OutlinedTextField(
                value = camera.url,
                onValueChange = onUrlChange,
                label = { Text("RTSP URL") },
                placeholder = { Text("rtsp://192.168.1.100:554/stream") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Display mode selector
            Text(
                text = "Display Mode",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoDisplayMode.entries.forEach { mode ->
                    val isSelected = camera.displayMode == mode
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = isSelected,
                                onClick = { onDisplayModeChange(mode) },
                                role = Role.RadioButton
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.toDisplayString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extension to get display string for BrightnessMode.
 */
private fun BrightnessMode.toDisplayString(): String = when (this) {
    BrightnessMode.AUTO -> "Auto (System)"
    BrightnessMode.MINIMUM -> "Minimum"
    BrightnessMode.CUSTOM -> "Custom"
}

/**
 * Extension to get display string for VideoDisplayMode.
 */
private fun VideoDisplayMode.toDisplayString(): String = when (this) {
    VideoDisplayMode.FIT -> "Fit"
    VideoDisplayMode.FILL -> "Fill"
    VideoDisplayMode.CROP -> "Crop"
}

/**
 * Extension to get description for VideoDisplayMode.
 */
private fun VideoDisplayMode.toDescription(): String = when (this) {
    VideoDisplayMode.FIT -> "Video fits inside screen, may show black bars"
    VideoDisplayMode.FILL -> "Video fills entire screen, may be stretched"
    VideoDisplayMode.CROP -> "Video fills screen maintaining ratio, may be cropped"
}
