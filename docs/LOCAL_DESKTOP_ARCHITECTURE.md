# LynOS local desktop workspace architecture

## Product boundary

LynOS is a personal, offline-first Android launcher/workspace APK. Mnemos is an independent memory tool/plugin in its own repository. PusingCuk is an independent language/runtime. LynOS integrates local capabilities through adapters and documented contracts; it must not absorb either project's repository or claim those backends work until connected.

## One desktop-like view, three cooperating layers

1. **Workspace shell** — navigation, panel layout, focus, window/panel sizing, status, and Android app launching. It is the visual desktop surface.
2. **Mnemos Local** — granted-folder indexing, local search, matching, graph exploration, source preview, and evidence-backed synthesis.
3. **Cuk terminal** — a terminal UI connected to a real compatible PusingCuk runtime. Until an Android-compatible runtime is integrated and exercised, the UI must clearly label the terminal as a restricted workspace console and source checking as validation only, not execution.

These are cooperating panels in one workspace, not three unrelated apps. The first usable layout may use tabs or stacked/resizable panels, but all must share navigation and preserve state when changing focus.

## Offline memory indexing

- Use Android Storage Access Framework folder/document grants. Persist URI permissions where Android permits it; do not claim access to the entire device storage.
- Make scanning explicit, incremental, cancellable, and visible with progress/errors. Default to metadata and supported text extraction; exclude app-private data, protected locations, and unsupported binary formats.
- Store the local index in the app's private database. Track URI/path, display name, MIME type, size, modified time/fingerprint, indexed time, extraction status, and source snippet locations.
- Detect changed, moved, removed, unreadable, oversized, and unsupported sources without silently deleting knowledge derived from them.
- Search should combine lexical matching first with metadata filters and graph traversal. Any semantic matching/synthesis engine must be local/offline for the offline milestone and must cite source evidence; do not fake semantic results with keyword search.
- Do not upload user files, invoke a remote model, or require an account in offline mode.

## Integration boundary

Create adapters around the local Mnemos contract rather than copying Python modules into Kotlin. Keep the native Android implementation and portable Mnemos data formats separate. For Cuk, define a runtime adapter boundary and report runtime availability/errors honestly; do not run arbitrary source in a fake console or silently invoke a network service.

## Privacy and lifecycle

Scanning only covers user-granted roots. Make indexed roots and file types visible, support excluding a root, and provide a way to clear derived indexes if the user asks. This is different from deleting original files or erasing all historical memory evidence. Avoid destructive memory restore/delete actions as the primary workflow; export is portability/recovery. Protect local data with app-private storage and keep cloud sync disabled until deliberately implemented.

## Acceptance criteria

- Fresh install launches with no network and no account.
- User can grant a folder, start/cancel indexing, and see progress plus skipped/error counts.
- Restart resumes from persisted index state without re-reading every unchanged file.
- Search results show source names/snippets and can open the original through Android's granted URI.
- Matching/summary results expose the evidence used and label lexical results versus semantic synthesis accurately.
- Workspace panels can switch focus without losing their state.
- Cuk source is never represented as executed unless an Android-compatible runtime actually executed it.
- No automatic APK/CI build is triggered as part of architecture/documentation work.
