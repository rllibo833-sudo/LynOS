---
name: android-feature
description: Implement end-to-end LynOS Android features and UI changes in Kotlin and Jetpack Compose. Use for feature requests, UI behavior, navigation, app launcher behavior, settings, and app state.
---

# LynOS Android Feature Implementation

## When to use
Use for a requested feature, missing UI behavior, navigation flow, settings behavior, launcher/App Space work, or Compose screen change.

## Workflow
1. Read README.md, AndroidManifest.xml, app/build.gradle.kts, and the relevant Kotlin source. Trace where state originates and how the user action reaches persistence or Android APIs.
2. Implement behavior, not just visual scaffolding. Wire UI events to state, persistence, navigation, and error states.
3. Follow existing Compose patterns and dependency choices; do not add libraries casually.
4. Cover loading/empty/success/failure states, recomposition correctness, lifecycle behavior, keyboard/accessibility semantics, and reduced-motion preference where animation is involved.
5. Keep Android behavior within supported SDK APIs. Check manifest declarations and runtime permission flow before adding platform access.
6. Add tests for pure logic and regression behavior when feasible. Never fabricate app inventory, database totals, device status, or success states.
7. Run available Gradle tests and assembleDebug. Inspect CI or build output before claiming success.

## Quality bar
- Feature is reachable through the app's real navigation.
- UI state reflects actual source data.
- User actions have visible success/failure outcomes.
- Existing behavior and min SDK 26 remain intact.
- Do not replace implementation with a README-only description or a fake/demo interaction.
- Do not claim real-device verification from CI compilation alone.

## LynOS boundaries
LynOS is a native Android launcher/workspace APK. It is not an operating-system replacement. Do not pretend an ordinary app can silently change system settings, read other apps' notifications, or execute arbitrary shell commands without explicit supported mechanisms.