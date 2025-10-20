# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LockDemo is a basic Android application written in Kotlin. This is a simple "Hello World" style project with standard Android project structure using Gradle build system.

## Build Commands

### Building the Project
```bash
# Build the project (using Gradle wrapper)
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Installation
```bash
# Install debug APK to connected device/emulator
./gradlew installDebug

# Install release APK to connected device/emulator
./gradlew installRelease
```

### Cleaning
```bash
# Clean build artifacts
./gradlew clean
```

## Project Structure

- **app/src/main/java/xyz/junerver/android/lockdemo/**: Main application source code
  - `MainActivity.kt`: Main activity with edge-to-edge display handling
- **app/src/test/**: Unit tests
- **app/src/androidTest/**: Instrumented tests
- **app/src/main/res/**: Android resources (layouts, values, drawables)
- **gradle/**: Gradle configuration including version catalog (libs.versions.toml)

## Architecture Notes

- Single-activity architecture using standard Android components
- Kotlin-based project with Java 11 compatibility
- Uses ConstraintLayout for UI layout
- Implements edge-to-edge display support
- Standard Android project structure following modern practices

## Development Configuration

- **Compile SDK**: 35
- **Min SDK**: 24
- **Target SDK**: 35
- **Kotlin Version**: 2.0.21
- **Java Compatibility**: Version 11
- **Application ID**: xyz.junerver.android.lockdemo

## Dependencies

Core Android dependencies managed through version catalog:
- AndroidX Core KTX, AppCompat, Activity, ConstraintLayout
- Material Design components
- Standard testing libraries (JUnit, Espresso)