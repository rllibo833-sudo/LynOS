# LynOS Agent Instructions

## Mission
Build and improve the actual LynOS Android application, not just audit or document it. Treat LynOS as a Kotlin + Jetpack Compose Android launcher/workspace with local Mnemos SQLite storage and Cuk Studio. The deliverable is working source code and a verified installable APK.

## Default behavior
- For a concrete feature or bug request, inspect the relevant implementation, implement the change, add/update tests where practical, and run the strongest available checks. Do not stop after an audit, plan, checklist, or recommendation.
- If the user says "fix", "build", "implement", or "continue", make code changes unless blocked by a real missing prerequisite. Report the actual changed files and test/build outcomes.
- Keep changes focused, but complete the end-to-end path: UI, state, persistence, error handling, accessibility, and tests as applicable.
- Do not ask the user to write code or run commands that the agent can run itself. Ask only when an essential product decision cannot be inferred safely.
- Never claim a feature works merely because code compiles or a structural check passes. Separate compiled, unit-tested, instrumented, and real-device-verified behavior.
- Do not delete existing user work or silently weaken tests to make CI green.

## Platform and architecture constraints
- Native Kotlin, Jetpack Compose, Android SDK 35, min SDK 26, Java 17. No Flutter/Dart.
- Keep core functionality local-first and usable without cloud services, hosted runners at runtime, or paid API keys.
- LynOS is an APK/launcher, not a replacement Android operating system. Do not claim it replaces system UI or bypasses Android security restrictions.
- Mnemos uses on-device SQLite. Preserve transaction boundaries, referential integrity, additive restore behavior, validation, and rollback guarantees.
- Cuk Studio's workspace console is currently a restricted in-app command console, not a Linux shell or a .cuk runtime. Never label structural validation as source execution.
- Request only Android permissions that are necessary, explain user-facing permission behavior, and keep secrets/signing keys out of source control.

## Required execution loop
1. Read README.md and the relevant code before editing.
2. Identify the actual behavior gap and trace the full call path.
3. Implement the smallest complete fix/feature; avoid docs-only substitutes for code.
4. Add regression tests for changed behavior when the project supports them.
5. Run available tests and Android build/signature checks. If a toolchain is unavailable, state exactly what was and was not verified.
6. Inspect the final diff for unrelated edits, unsafe permissions, leaked secrets, and false claims.
7. Summarize changed files, concrete behavior now present, test/build results, and remaining real-device checks.

## Build commands
CI currently uses Gradle 8.9, JDK 17, Android SDK platform/build-tools 35:
- `gradle --no-daemon :app:assembleDebug`
- `gradle --no-daemon :app:assembleRelease`
- `gradle --no-daemon test` (if JVM tests exist)
A Gradle wrapper is not currently checked in; do not claim `./gradlew` exists unless you add and verify it.

## Skill selection
Use the relevant skill when implementing Android features, debugging Kotlin/Compose, working on Mnemos persistence, or building/releasing APKs:
- `.agents/skills/android-feature/SKILL.md`
- `.agents/skills/kotlin-debugging/SKILL.md`
- `.agents/skills/mnemos-storage/SKILL.md`
- `.agents/skills/android-build-release/SKILL.md`

## Completion report
Be concise and evidence-based: implementation, files changed, checks actually run, CI link/status if available, and any unverified device-specific behavior. Do not report an audit as implementation.