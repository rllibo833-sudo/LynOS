# LynOS

LynOS is a **native Android shell/workspace prototype** built on Android. It is not a bootloader, custom kernel, or replacement operating system.

## Visual direction

The source of truth is [Mnemos_ Jaringan Pengetahuan Futuristik.png](./Mnemos_%20Jaringan%20Pengetahuan%20Futuristik.png).

- Midnight Slate `#182333`, Deep Navy `#24364D`, Neural Cyan `#38CFFF`, Night Blue `#527DB8`.
- Neural scene is rendered natively with Jetpack Compose Canvas: atmospheric background, perspective guides, curved graph connections, pulsing hubs and subtle near-field particles.
- The scene honors the Reduced motion setting with slower/steadier animation; the visual remains an early approximation until screenshot comparison and target-device profiling are completed.
- Home dashboard memory and relation totals are read from the on-device SQLite database; no sample statistics are fabricated.

## Native implementation

- Kotlin + Jetpack Compose; no Flutter, Dart, Termux or cloud requirement for the local shell.
- Minimum Android API 26; SDK 35; Java 17.
- Home includes navigation to Mnemos, Cuk Studio, Settings and device Status.
- Mnemos uses SQLite on-device for notes, search and note-to-note links. Note updates fail explicitly if the referenced row disappeared; undirected relations use canonical endpoint ordering to avoid reversed duplicate edges. Broader device testing and schema migration tests remain outstanding.
- Cuk Studio currently has an editor and a source-wrapper check only. **It does not yet execute the full .cuk language runtime.** Integration with the native PusingCuk runtime remains required.
- Cloud sync is not implemented. The app remains local-first.
- Status shows Android-reported battery level and local time. LynOS does not read notifications from other apps; that requires explicit Notification Listener access, which is not implemented.

## Download an APK

Open [GitHub Actions](https://github.com/rllibo833-sudo/LynOS/actions), choose the latest successful **Build installable Android APKs** run, then download one of its artifacts. GitHub downloads artifacts as ZIP files; extract the ZIP first to get the APK.

- **LynOS-debug-apk** contains `app-debug.apk`, intended for testing.
- **LynOS-release-apk** contains `app-release.apk`, signed by a temporary CI key so Android can install it for evaluation.

The CI release signing key is intentionally disposable. A new CI run uses a different key, so its release APK cannot update an installation signed by an earlier run without uninstalling the older app first. For durable updates and distribution, configure a private persistent release keystore; never commit the keystore or passwords to Git.

A successful CI build proves compilation and APK-signature verification, not that every feature has been tested on a physical phone.

## Build locally

Open the project in Android Studio with Android SDK 35 installed and sync the Gradle project. CI currently installs Gradle 8.9 directly; a Gradle wrapper has not yet been added.

## Remaining acceptance work

1. Verify latest CI build, artifact signatures and install/launch on a real Android device.
2. Test Mnemos save/search/link across app restarts and improve relationship navigation/deletion.
3. Integrate the actual native PusingCuk runtime rather than treating source validation as execution.
4. Add a persistent private release-signing workflow for safe updates.
5. Compare screenshots against the reference and measure animation performance on the target phone.
