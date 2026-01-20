# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Key Documentation Files:**
- **[CLAUDE.md](CLAUDE.md)** - Project overview, objectives, architecture, session management
- **[SESSION_STATE.md](SESSION_STATE.md)** - Current session status and recent work
- **[.claude/REFERENCE.md](.claude/REFERENCE.md)** - Quick reference: URLs, credentials, cmdlets
- **[README.md](README.md)** - Installation and setup guide

---

## Project Context

**Project Name:** RTSP to Tablet
**Tech Stack:** Android (Kotlin), ExoPlayer/VLC-Android
**Primary Language(s):** Kotlin
**Key Dependencies:**
- ExoPlayer ou VLC-Android (lecture RTSP)
- AndroidX (composants UI modernes)
- Jetpack Compose ou View XML (UI)

**Architecture Pattern:** MVVM (Model-View-ViewModel)
**Development Environment:** Android Studio, SDK minimum 24 (Android 7.0+)

---

## Project Objectives

### Primary Goal
Application Android permettant d'afficher un flux vidéo réseau (RTSP) en plein écran sur tablette, avec gestion intelligente de l'écran pour économiser l'énergie.

### Core Features

#### 1. Lecture de flux RTSP
- Support des URLs RTSP (rtsp://...)
- Reconnexion automatique en cas de perte de connexion
- Gestion des erreurs de flux (timeout, format non supporté)
- **Future:** Support d'autres protocoles (HLS, RTMP, HTTP streams)

#### 2. Affichage plein écran
- Mode immersif (masque barre de statut et navigation)
- Support des orientations portrait et paysage
- Adaptation automatique du ratio vidéo (fit/fill/crop options)
- Pas d'UI superposée pendant la lecture (sauf gestes)

#### 3. Gestion de l'écran (Feature principale)
- **Permettre l'extinction de l'écran** même pendant la lecture du flux
  - Option pour désactiver le FLAG_KEEP_SCREEN_ON
  - Paramétrage du délai avant extinction (suivre les paramètres système)
- **Réduction de luminosité au minimum**
  - Option pour réduire automatiquement la luminosité
  - Possibilité de définir un niveau de luminosité personnalisé
- **Reprise sur réveil**
  - Quand l'utilisateur rallume l'écran, le flux vidéo est immédiatement visible
  - Le flux continue de jouer en arrière-plan (ou reprend instantanément)
  - Pas de délai de reconnexion visible pour l'utilisateur

#### 4. Configuration
- Écran de paramètres pour :
  - URL du flux RTSP
  - Comportement de l'écran (autoriser extinction oui/non)
  - Niveau de luminosité (auto/min/personnalisé)
  - Options de reconnexion automatique
  - Mode d'affichage vidéo (fit/fill/crop)

### Use Cases
- Affichage permanent d'une caméra de surveillance sur tablette murale
- Moniteur bébé sur tablette de chevet (écran éteint la nuit, visible au réveil)
- Dashboard vidéo avec économie d'énergie

---

## Technical Architecture

### Key Components

```
app/
├── src/main/java/com/[package]/
│   ├── MainActivity.kt           # Entry point, gestion du cycle de vie
│   ├── ui/
│   │   ├── player/
│   │   │   ├── PlayerActivity.kt # Activité de lecture plein écran
│   │   │   ├── PlayerViewModel.kt
│   │   │   └── PlayerScreen.kt   # (si Compose)
│   │   └── settings/
│   │       ├── SettingsActivity.kt
│   │       └── SettingsViewModel.kt
│   ├── player/
│   │   ├── RtspPlayer.kt         # Abstraction du player
│   │   ├── PlayerState.kt        # États du player
│   │   └── PlayerConfig.kt       # Configuration
│   ├── screen/
│   │   ├── ScreenManager.kt      # Gestion écran/luminosité
│   │   └── WakeLockManager.kt    # Gestion wake locks
│   ├── data/
│   │   ├── PreferencesRepository.kt
│   │   └── StreamConfig.kt
│   └── util/
│       └── Extensions.kt
├── src/main/res/
│   ├── layout/                   # (si View XML)
│   └── values/
└── build.gradle.kts
```

### Screen Management Strategy

```kotlin
// Concept de gestion de l'écran
class ScreenManager(private val activity: Activity) {

    // Désactiver le keep screen on (permettre extinction)
    fun allowScreenOff() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Réduire luminosité au minimum
    fun setMinimumBrightness() {
        val params = activity.window.attributes
        params.screenBrightness = 0.01f // Minimum visible
        activity.window.attributes = params
    }

    // Restaurer luminosité normale
    fun restoreBrightness() {
        val params = activity.window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params
    }
}
```

### Player Lifecycle

```
IDLE -> BUFFERING -> PLAYING -> PAUSED/STOPPED
                         |
                         v
              (écran éteint, flux maintenu ou mis en pause)
                         |
                         v
              (écran rallumé, reprise immédiate)
```

---

## File Encoding Standards

- **All files:** UTF-8 with LF (Unix) line endings
- **Timestamps:** ISO 8601 (YYYY-MM-DD HH:mm)
- **Time format:** 24-hour (HH:mm)

---

## Claude Code Session Management

### Quick Start (TL;DR)

**Continue work:** `"continue"` or `"let's continue"`
**New session:** `"new session: Feature Name"` or `"start new session"`

**Claude handles everything automatically** - no need to manage session numbers or files manually.

---

### Session File Structure

**Two-Tier System:**
1. **SESSION_STATE.md** (root) - Overview and index of all sessions
2. **.claude/sessions/SESSION_XXX_[name].md** - Detailed session files

**Naming:** `SESSION_001_project_setup.md` (three digits, 001-999)

**Session Limits (Recommendations):**
- Max tasks: 20-25 per session
- Max files modified: 15-20 per session
- Recommended duration: 2-4 hours

---

### Automatic Session Workflow

#### 1. Session Start
- Read CLAUDE.md, SESSION_STATE.md, current session file
- Display status and next tasks

#### 2. During Development (AUTO-UPDATE)
**Individual Session File:**
- Mark completed tasks immediately
- Log technical decisions and issues in real-time
- Track all modified files
- Document all code created

**SESSION_STATE.md:**
- Update timestamp and session reference
- Update current status
- Add to recent sessions summary

#### 3. Session File Template

```markdown
# Session XXX: [Feature Name]

## Date: YYYY-MM-DD
## Duration: [Start - Current]
## Goal: [Brief description]

## Completed Tasks
- [x] Task 1 (HH:mm)
- [ ] Task 2 - In progress

## Current Status
**Currently working on:** [Task]
**Progress:** [Status]

## Next Steps
1. [ ] Next immediate task
2. [ ] Following task

## Technical Decisions
- **Decision:** [What]
  - **Reason:** [Why]
  - **Trade-offs:** [Pros/cons]

## Issues & Solutions
- **Issue:** [Problem]
  - **Solution:** [Resolution]
  - **Root cause:** [Why]

## Files Modified
### Created
- path/file.kt - [Description]
### Updated
- path/file.kt - [Changes]

## Documentation Created/Updated
- [ ] [file].EXPLAIN.md - Created/Updated
- Files documented: X/Y (Z%)

## Dependencies Added
- package:version - [Reason]

## Testing Notes
- [ ] Tests written/passing
- **Coverage:** [%]

## Session Summary
[Paragraph summarizing accomplishments]

## Handoff Notes
- **Critical context:** [Must-know info]
- **Blockers:** [If any]
- **Next steps:** [Recommendations]
```

---

### Session Management Rules

#### MANDATORY Actions:
1. Always read CLAUDE.md first for context
2. Always read current session file
3. Update session in real-time as tasks complete
4. Document all code (headers, functions, .EXPLAIN.md)
5. Never lose context between messages
6. Auto-save progress every 10-15 minutes
7. Verify documentation before marking tasks complete

#### When to Create New Session:
- New major feature/module
- Completed session goal
- Different project area
- After long break
- Approaching session limits

---

### Common Commands

**Continue:** "continue", "let's continue", "keep going"
**New session:** "new session: [name]", "start new session"
**Save:** "save progress", "checkpoint"
**Update:** "update session", "update SESSION_STATE.md"
**Document:** "document files", "create EXPLAIN files"
**Audit:** "check documentation", "audit docs"

---

## Documentation Standards

### Overview
**Every code file MUST have complete documentation before task is marked complete.**

### Required Documentation Elements

#### 1. File Header (All Kotlin Files)
```kotlin
/**
 * @file filename.kt
 * @description Brief file purpose
 * @session SESSION_XXX
 * @created YYYY-MM-DD
 * @author [name/team]
 */
```

#### 2. Function Documentation
```kotlin
/**
 * Brief function description
 *
 * @param paramName Parameter description
 * @return Return description
 * @throws Exception Error conditions
 * @sample
 * functionName(arg) // => result
 * @session SESSION_XXX
 */
```

#### 3. .EXPLAIN.md Files (Scripts/Modules)
**Create for:** All complex modules, utilities, managers

**Template:**
```markdown
# [Filename] - Explanation

## Purpose
[What this does]

## How It Works
[Step-by-step explanation]

## Usage
```kotlin
[Code examples]
```

## Key Functions
### functionName()
[Description, params, returns]

## Error Handling
[Common issues and solutions]

## Session History
- SESSION_XXX: Created
```

---

### Documentation Checklist
Before marking any task complete, verify:
- [ ] File header present
- [ ] All functions documented (description, params, returns, examples)
- [ ] All classes documented (properties, methods, usage)
- [ ] Complex sections explained
- [ ] Inline comments for non-obvious logic
- [ ] .EXPLAIN.md created/updated (for complex modules)
- [ ] Error cases documented

---

## Git Workflow Integration

### Branch Naming
**Format:** `feature/session-XXX-brief-description`
**Examples:** `feature/session-001-rtsp-player`, `bugfix/session-003-screen-wake`

### Commit Messages
```
Session XXX: [Brief summary]

[Details]

Changes:
- Change 1
- Change 2

Documentation:
- Updated [file].EXPLAIN.md

Session: SESSION_XXX
```

### Tagging Completed Sessions
```bash
git tag -a session-XXX-complete -m "Session XXX: [Feature] - Complete"
git push origin session-XXX-complete
```

---

## Android-Specific Guidelines

### Permissions Required
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Key Android Components
- **WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON** - À désactiver pour permettre extinction
- **WindowManager.LayoutParams.screenBrightness** - Contrôle luminosité (0.0 à 1.0)
- **BroadcastReceiver (ACTION_SCREEN_ON/OFF)** - Détecter allumage/extinction écran
- **Foreground Service** - Maintenir le flux actif en arrière-plan si nécessaire

### Testing Checklist
- [ ] Test sur tablette physique (émulateur limité pour écran)
- [ ] Test de reconnexion après perte réseau
- [ ] Test cycle extinction/allumage écran
- [ ] Test rotation écran pendant lecture
- [ ] Test mémoire sur lecture longue durée

---

## Additional Resources

- **Quick Start:** [.claude/QUICKSTART.md](.claude/QUICKSTART.md)
- **Reference:** [.claude/REFERENCE.md](.claude/REFERENCE.md)
- **Templates:** [.claude/templates/](.claude/templates/)

---

**Last Updated:** 2026-01-20
**Version:** 1.0.0
