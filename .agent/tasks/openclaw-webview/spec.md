# Task Spec: openclaw-webview

## Metadata
- Task ID: openclaw-webview
- Created: 2026-04-02T12:02:37+00:00
- Repo root: /Users/germankochnev/clawmer
- Working directory at init: /Users/germankochnev/clawmer

## Guidance sources
- CLAUDE.md

## Original task statement
Implement OpenClaw WebView UI: Replace current chat/settings UI with a simple WebView-based app. First screen: user enters the OpenClaw control UI URL (e.g. http://localhost:18789/config). Second screen: WebView loads that URL. Strip out all existing complex UI screens (chat, cron, settings) and replace with this minimal two-screen flow.

## Acceptance criteria
- AC1: URL Entry Screen exists and is the app launch screen. On app launch, a screen is displayed with a text field for entering a URL, a sensible default/hint (e.g. `http://localhost:18789/config`), and a button to navigate to the WebView screen.
- AC2: WebView Screen loads the entered URL. After submitting a URL, a WebView screen opens and loads the provided URL with JavaScript enabled.
- AC3: Navigation between screens works. Two-screen flow: URL Entry -> WebView. System back from WebView returns to URL Entry.
- AC4: INTERNET permission is declared in AndroidManifest.xml.
- AC5: App builds successfully (`./gradlew assembleDebug` exits 0).
- AC6: No leftover complex UI code. No chat, cron, settings, agent management screens, or unused data layer code remain.

## Constraints
- Keep existing Material 3 / Compose theming infrastructure
- Use `android.webkit.WebView` wrapped in Compose via `AndroidView`
- Single-activity architecture with Compose navigation
- No new external dependencies (WebView is part of Android SDK)
- Navigation via simple state hoisting — keep it minimal
- minSdk 24, targetSdk 36, Kotlin, Jetpack Compose

## Non-goals
- No custom WebView chrome (address bar, navigation buttons beyond system back)
- No URL persistence or history
- No SSL certificate handling customization
- No file upload/download support
- No WebSocket or HTTP client libraries
- No data layer (repositories, models, remote clients)

## Verification plan
- Build: `./gradlew assembleDebug` must exit 0
- Unit tests: not applicable (pure UI, minimal logic)
- Integration tests: not applicable
- Lint: `./gradlew lint` should pass without errors
- Manual checks: Inspect source files for AC1-AC4, AC6
