# RTSP to Tablet

Android application for displaying RTSP video streams on tablets with intelligent screen management.

## Features

- **RTSP Streaming** - Play RTSP video streams using Media3 ExoPlayer
- **Full-screen Playback** - Immersive mode with hidden system bars
- **Overlay Controls** - Tap to show/hide controls
  - Close button (top-left)
  - Settings button (top-right)
  - Camera navigation arrows (left/right) - prepared for future multi-camera support
- **Screen Management**
  - Allow screen to turn off during playback (energy saving)
  - Brightness control (Auto/Minimum/Custom)
- **Auto-reconnection** - Automatic retry on connection failure
- **Configurable Settings**
  - RTSP URL
  - Screen behavior
  - Video display mode (Fit/Fill/Crop)
  - Reconnection delay

## Requirements

- Android 7.0+ (API 24)
- Network access to RTSP stream

## Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and deploy to device

## Usage

1. Launch the app
2. Tap the screen to show controls
3. Tap the settings icon (gear) to configure
4. Enter your RTSP URL (e.g., `rtsp://192.168.1.100:554/stream`)
5. Return to player - stream will start automatically

## Configuration Options

| Setting | Description |
|---------|-------------|
| RTSP URL | Stream address (rtsp://...) |
| Allow screen off | Let screen turn off during playback |
| Brightness mode | Auto, Minimum, or Custom level |
| Video display | Fit (letterbox), Fill (stretch), Crop |
| Auto-reconnect | Retry on connection failure |
| Reconnect delay | Wait time before retry (1-10 seconds) |

## Use Cases

- **Surveillance** - Wall-mounted tablet displaying security camera
- **Baby monitor** - Bedside tablet with screen off at night
- **Dashboard** - Video feed with energy-efficient display

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Video:** Media3 ExoPlayer with RTSP extension
- **Storage:** DataStore Preferences
- **Architecture:** MVVM

## Project Structure

```
app/src/main/java/com/music/rtsptotablet/
├── MainActivity.kt              # Entry point with navigation
├── RtspToTabletApplication.kt   # Application class
├── navigation/
│   └── Screen.kt                # Navigation routes
├── data/
│   ├── model/AppSettings.kt     # Settings data model
│   └── repository/PreferencesRepository.kt
├── player/
│   └── PlayerState.kt           # Player state definitions
├── screen/
│   └── ScreenManager.kt         # Screen/brightness control
└── ui/
    ├── theme/Theme.kt           # Material 3 theme
    ├── player/
    │   ├── PlayerViewModel.kt   # Player logic
    │   └── PlayerScreen.kt      # Player UI
    └── settings/
        ├── SettingsViewModel.kt # Settings logic
        └── SettingsScreen.kt    # Settings UI
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request
