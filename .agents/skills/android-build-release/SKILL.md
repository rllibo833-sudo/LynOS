---
name: android-build-release
description: Build, package, sign, and verify LynOS Android APKs using the repository's actual Gradle and CI toolchain.
---

# LynOS Android Build and Release

## Known toolchain
- JDK 17, Gradle 8.9, Android SDK platform 35, build-tools 35.0.0.
- CI builds debug and release APKs and verifies both signatures.
- The repository currently has no Gradle wrapper; do not invoke ./gradlew unless one has been added and verified.
- CI release signing uses a disposable key unless a private persistent keystore is configured.

## Workflow
1. Inspect the current workflow and build configuration before changing toolchain versions.
2. For code changes, run JVM tests when present and assemble debug/release APKs.
3. Verify artifact existence and APK signatures using Android build-tools.
4. Check CI result on the commit being reported; do not infer green status from a previous run.
5. Never commit signing keys, passwords, or credentials. Do not echo secrets in logs.
6. Distinguish build success, signature verification, artifact publication, installation, and real-device behavior as separate levels of evidence.

## Release safety
- Do not describe a disposable CI-signed APK as suitable for durable app updates.
- A new ephemeral signing key cannot update an app signed by a previous run; users may need to uninstall the old test APK first.
- Do not publish a release as device-verified unless a physical-device installation and launch were actually performed.
- Preserve least-privilege workflow permissions and pin/upgrade third-party actions deliberately.