---
name: kotlin-debugging
description: Diagnose and fix Kotlin, Jetpack Compose, Android lifecycle, Gradle, and CI failures in LynOS. Use for crashes, compile errors, broken interactions, regressions, and failing checks.
---

# Kotlin and Compose Debugging

## Workflow
1. Reproduce the failure from the actual exception, CI logs, test, or source path. Do not guess based on a symptom alone.
2. Trace the failure through callers, state ownership, lifecycle, database calls, and platform API usage.
3. Fix the underlying cause with the smallest robust patch. Avoid blanket catch blocks, swallowed exceptions, arbitrary delays, and weakened assertions.
4. Add a regression test or a deterministic reproducer. Prefer pure Kotlin tests for parsing/validation/business rules when possible.
5. Run the narrowest relevant test first, then broader tests and Android assembly.
6. Review adjacent code for the same defect pattern without expanding into unrelated refactors.

## Compose checks
- Ensure state is observable and updated immutably.
- Avoid performing side effects directly during recomposition.
- Use lifecycle-aware collection where applicable.
- Keep stable keys for dynamic lists and handle empty/error states.
- Check recomposition, stale closures, and repeated event invocation.

## Data and Android checks
- For SQLite failures, inspect schema version, migration path, transaction scope, and query edge cases.
- For Android API failures, verify min SDK 26 compatibility, manifest declarations, runtime permission needs, and lifecycle safety.
- For Gradle failures, preserve the original error and distinguish dependency resolution, Kotlin compilation, resource processing, packaging, and signature verification.

## Reporting
State the root cause, code changed, regression coverage, exact commands/checks run, and any remaining unverified behavior. Never report a speculative cause as confirmed.