# LynOS

LynOS is a personal Android launcher/workspace project targeting a complete, installable APK with a familiar Android experience, a local Mnemos knowledge graph, and Cuk Studio. The goal is to make LynOS usable as the device's default Home app without requiring bootloader unlocking.

**Platform boundary:** installing LynOS as an APK and setting it as the default launcher does not replace Android itself. Replacing the operating system or system UI requires OEM-supported system-level access/firmware changes; an ordinary APK cannot do that on a locked stock device. Development and distribution should target a complete APK first, without assuming bootloader access.

## Visual direction

The source of truth is [Mnemos_ Jaringan Pengetahuan Futuristik.png](./Mnemos_%20Jaringan%20Pengetahuan%20Futuristik.png).

- Midnight Slate `#182333`, Deep Navy `#24364D`, Neural Cyan `#38CFFF`, Night Blue `#527DB8`.
- Neural scene is rendered natively with Jetpack Compose Canvas: atmospheric background, perspective guides, curved graph connections, pulsing hubs and subtle near-field particles.
- The Reduced motion preference is persisted on-device and freezes the animated scene into a static composition. The visual remains an early approximation until screenshot comparison and target-device profiling are completed.
- Home dashboard memory and relation totals are read from the on-device SQLite database; no sample statistics are fabricated.

## Native implementation

- Kotlin + Jetpack Compose; no Flutter, Dart, Termux or cloud requirement for the local shell.
- Minimum Android API 26; SDK 35; Java 17.
- Home includes navigation to Mnemos, Cuk Studio, Settings and device Status.
- Mnemos uses SQLite on-device for notes, search and note-to-note links, and can export a snapshot through Android's document picker as JSON (`lynos-mnemos-export-v1`) or readable Markdown. This is deliberately a LynOS-specific snapshot format; it is not yet compatible with Mnemos-RLI’s `mnemos-export-v1` interchange contract. Export includes notes and graph relations; it does not delete or modify the local database. Note updates fail explicitly if the referenced row disappeared; undirected relations use canonical endpoint ordering to avoid reversed duplicate edges. Database schema version 2 migrates legacy graph links by canonicalizing endpoint order, normalizing blank relations, and discarding self-links/duplicates. Related-note queries deduplicate results and exclude the selected note itself. Search input treats `%` and `_` literally instead of allowing them to act as SQL wildcards. Link creation now validates both note endpoints inside a database transaction and rejects relation labels longer than 128 characters; duplicate undirected links remain idempotent. Dashboard totals reload after successful note saves, links and deletions. Migration and query behavior still need device/database-level verification.
- Cuk Studio has a local `.cuk` source editor, a wrapper-structure check, and a small LynOS workspace terminal. Terminal commands currently include `help`, `date`, `status`, `notes`, `links`, `find <text>`, `note <title> :: <body>`, and `clear`; note commands read/write the local Mnemos SQLite database. This is a restricted app command console, **not** an Android/Linux shell, and it does not yet execute `.cuk` source. The wrapper check validates structure only.
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
2. Test Mnemos save/search/link/unlink and JSON/Markdown export across app restarts, including filtered searches, export cancellation/write errors, and stale selections.
3. Decide and implement a safe local `.cuk` execution backend for Cuk Studio; until that exists, keep terminal capabilities clearly scoped to LynOS workspace commands and never label source checks as execution.
4. Add a persistent private release-signing workflow for safe updates.
5. Compare screenshots against the reference image and measure animation performance on the target phone.

## Data/backend hardening

- Mnemos remains offline-first: the device SQLite database is the source of truth; the app does not require an API or account to create/search/edit/delete notes or maintain graph links.
- Schema v3 adds indexes for recent-note ordering, reverse-edge traversal, and relation filtering. Existing v1 databases are normalized to schema v2 first, then receive the v3 indexes; fresh installs create the current schema directly.
- Link writes validate both endpoints inside a transaction, canonicalize undirected edges, reject self-links, and preserve foreign-key cascade deletion.
- Database calls made by the UI are dispatched to Dispatchers.IO; do not move database work onto the main thread as features grow.
- Compatibility target remains Android API 26+ / Java 17. Any future Room migration should preserve the existing mnemos.db data rather than silently replacing it.

## Reuse audit

- [Cukaccount](https://github.com/rllibo833-sudo/Cukaccount): its current README describes a portable browser-based CSV matching tool, while legacy Flutter/SQLite code remains in the repository. It is not a drop-in Android backend for LynOS; transferable principles include deterministic validation, explicit error states, and local-first processing, but legacy modules must be inspected before reuse.
- [Toolbox](https://github.com/rllibo833-sudo/Toolbox): a mobile-first web utility with calculation and input-validation patterns. It is not an Android backend dependency and should not be copied wholesale.
- [pusingcuk](https://github.com/rllibo833-sudo/pusingcuk): a separate language/runtime project. Cuk Studio currently provides an editor and restricted workspace commands; it does not claim to execute .cuk source.

These projects are references, not runtime dependencies. LynOS should keep Android UI, persistence, and future sync/API layers separated so a future online backend can be added without sacrificing offline operation.
## Mnemos compatibility audit

The Claude-provided Mnemos assets have been reviewed against the native Android implementation. Python CLI, Rich terminal UI, `psutil`, agent registry, and MCP examples are not drop-in LynOS components; they assume a separate Python backend and APIs not present in the Kotlin app. The accepted direction is native SQLite-backed functionality, with native JSON/Markdown export now wired to Android's document picker. Tags and advanced graph analytics require explicit schema/API work before they can be advertised. Export and restore round-trip validation still needs to be performed on-device. See [the full compatibility audit](docs/MNEMOS_CLAUDE_ALIGNMENT.md) for per-file findings and implementation decisions.
