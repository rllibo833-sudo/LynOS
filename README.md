# LynOS

LynOS is a native Android shell/app prototype focused on a calm midnight-blue neural interface. It runs **on top of Android**; it is not a bootloader, custom kernel, or replacement OS.

## Device-first goals

- Target entry-level Android devices, including 4 GB RAM phones.
- Kotlin + Jetpack Compose; no Flutter, Dart, Termux, or cloud requirement for the home screen.
- Midnight slate surfaces (`#182333`), deep navy panels (`#24364D`), neural cyan (`#38CFFF`), and night blue (`#527DB8`).
- Draw a bounded neural graph with Canvas and a small, fixed node/edge set. Avoid blur-heavy effects, large particle systems, and continuous animation when the screen is not visible.
- Respect Android reduced-motion / animator settings where practical; keep touch targets and text readable.

## Planned integrations

- **PusingCuk:** reuse its native runtime through an explicit, tested integration boundary; do not copy compiler internals blindly.
- **Mnemos:** local-first knowledge store. Audit and document its SQLite schema before choosing a Kotlin-native port or bridge. Do not let two implementations write the same database concurrently.

## Build

Open this repository in Android Studio with Android SDK 35 installed, then sync and run the `app` configuration on an Android device or emulator. A Gradle wrapper will be added when its verified wrapper artifacts are available.

## Current scope

The first milestone is a buildable native shell with a custom animated neural-node home screen. Repository scaffolding is not a claim that APK builds or device performance have already been verified.
