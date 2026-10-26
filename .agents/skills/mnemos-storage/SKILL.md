---
name: mnemos-storage
description: Implement and debug LynOS Mnemos SQLite notes, search, graph relations, migrations, and JSON/Markdown import-export.
---

# Mnemos Storage and Graph Integrity

## Invariants
- Mnemos data is local-first and stored in the on-device SQLite database.
- Validate external backup structure before writing. Validate IDs, required fields, relation endpoints, and relation label limits.
- Additive restore must not overwrite existing local notes. Map imported source IDs to new local IDs and preserve relation endpoints through that mapping.
- Perform multi-row restore and graph updates transactionally. On failure, roll back all partial writes.
- Canonicalize undirected relation endpoints; reject self-links and avoid duplicate edges.
- Keep note/link counts derived from actual database state. Do not fabricate dashboard totals.
- Search must treat user input literally where documented; SQL wildcards must not unexpectedly broaden matches.
- Surface missing-row/stale-selection failures instead of silently reporting success.

## Workflow
1. Read MnemosDatabase.kt, its callers in MainActivity.kt, and README.md.
2. Trace the complete read/write path, schema, migration, and UI refresh behavior.
3. Implement the requested fix with explicit validation and transaction boundaries.
4. Add regression coverage for malformed input, duplicate IDs/edges, missing endpoints, rollback, migration, and stale records as relevant.
5. Build the Android app; note that compilation alone does not verify database migration behavior on a device.
6. Report verified behavior and any migration/device-level tests still needed.

## Backup compatibility
LynOS currently uses the versioned `lynos-mnemos-export-v1` snapshot format. Do not claim compatibility with other Mnemos interchange formats unless implemented and tested explicitly.