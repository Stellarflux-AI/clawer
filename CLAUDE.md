# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Clawmer is an Android app built with Kotlin and Jetpack Compose. It targets Android API 36 (minSdk 24) and uses Material 3 theming.

- Package: `com.stellarflux`
- Single module: `:app`
- Build system: Gradle with Kotlin DSL and version catalogs (`gradle/libs.versions.toml`)

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest      # Run unit tests for debug variant only
./gradlew lint                   # Run Android lint
./gradlew clean                  # Clean build outputs
```

## Architecture

- Entry point: `app/src/main/java/com/stellarflux/MainActivity.kt` — single Activity using `setContent` with Compose
- Theme: `app/src/main/java/com/stellarflux/ui/theme/` (Color.kt, Theme.kt, Type.kt) — Material 3 dynamic color theming
- Version catalog: `gradle/libs.versions.toml` — all dependency versions managed here
- Compose BOM manages Compose library versions collectively

## Key Configuration

- Java 11 source/target compatibility
- Kotlin Compose compiler plugin (via `libs.plugins.kotlin.compose`)
- AGP 9.1.0, Kotlin 2.2.10
- ProGuard/R8 minification disabled for release builds
