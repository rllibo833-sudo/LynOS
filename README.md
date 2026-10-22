# LynOS

LynOS is a **native Android shell/app** prototype built on top of Android. It is not a bootloader, custom kernel, or replacement operating system.

## Design reference

The visual source of truth is [Mnemos_ Jaringan Pengetahuan Futuristik.png](./Mnemos_%20Jaringan%20Pengetahuan%20Futuristik.png) in this repository.

- Midnight Slate: `#182333`
- Deep Navy: `#24364D`
- Neural Cyan: `#38CFFF`
- Night Blue: `#527DB8`
- Solid-enough surfaces for legible text, a bounded connected-node graph, and restrained moving light pulses.

## Device-first implementation

- Kotlin + Jetpack Compose; no Flutter, Dart, Termux, or cloud requirement for the shell.
- Minimum Android API 26; compile/target SDK 35; Java 17.
- The graph uses a fixed set of 8 nodes and 11 edges, drawn with Compose Canvas. It intentionally avoids blur-heavy shaders and unbounded particle systems for lower-end devices.
- Reduced-motion and lifecycle-aware animation handling remain hardening work; current animation is a prototype and device performance has not yet been measured.

## Build and download

Every push to `main`, pull request targeting `main`, or manual workflow dispatch runs [Android CI](./.github/workflows/android-ci.yml). The workflow installs JDK 17, Android SDK 35 and Gradle 8.9, then runs:

```sh
gradle --no-daemon :app:assembleDebug
```

When the workflow succeeds, download **LynOS-debug-apk** from that run's **Artifacts** section. This repository does not yet include a Gradle wrapper, so the CI workflow installs Gradle directly. A successful CI run is required before describing the APK as build-verified.

For local development, open the project in Android Studio with Android SDK 35 installed and sync the Gradle project.

## Integration roadmap

1. **Native shell:** establish repeatable APK build and verify the UI on a real Android device.
2. **Mnemos:** audit the existing local-first SQLite schema and define one clear ownership boundary before integrating it. Do not allow Kotlin and Python implementations to write the same database concurrently.
3. **PusingCuk:** integrate the native runtime behind a tested interface; do not blindly copy compiler internals or introduce a Python/Termux dependency into the Android shell.
4. **Hardening:** lifecycle-aware/reduced-motion animation, accessibility checks, crash-free launch, and memory/frame-time measurements on the target phone.

## Current status

The repository contains the initial Compose shell and an automated APK build workflow. **The build is not yet confirmed green**, and the APK is not claimed to be device-tested. The immediate acceptance gate is a successful CI build followed by installation and launch testing on the target phone.
