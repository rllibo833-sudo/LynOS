package org.lynos.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class KnowledgeNote(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long
)

/**
 * Offline-first storage for Mnemos. Notes are durable on-device; a future sync
 * layer can use stable IDs and updatedAt without making the app depend on a server.
 */
class MnemosDatabase(context: Context) : SQLiteOpenHelper(context, "mnemos.db", null, 3) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL DEFAULT '',
                updated_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE links (
                source_id INTEGER NOT NULL,
                target_id INTEGER NOT NULL,
                relation TEXT NOT NULL DEFAULT 'related',
                PRIMARY KEY(source_id, target_id, relation),
                FOREIGN KEY(source_id) REFERENCES notes(id) ON DELETE CASCADE,
                FOREIGN KEY(target_id) REFERENCES notes(id) ON DELETE CASCADE
            )""".trimIndent()
        )
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Normalize historical undirected edges. The temporary table keeps
            // foreign keys and the composite key while collapsing reversed rows.
            db.execSQL(
                """CREATE TABLE links_v2 (
                    source_id INTEGER NOT NULL,
                    target_id INTEGER NOT NULL,
                    relation TEXT NOT NULL DEFAULT 'related',
                    PRIMARY KEY(source_id, target_id, relation),
                    FOREIGN KEY(source_id) REFERENCES notes(id) ON DELETE CASCADE,
                    FOREIGN KEY(target_id) REFERENCES notes(id) ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL(
                """INSERT OR IGNORE INTO links_v2 (source_id, target_id, relation)
                   SELECT MIN(source_id, target_id), MAX(source_id, target_id),
                          COALESCE(NULLIF(TRIM(relation), ''), 'related')
                   FROM links
                   WHERE source_id != target_id
                   GROUP BY MIN(source_id, target_id), MAX(source_id, target_id),
                            COALESCE(NULLIF(TRIM(relation), ''), 'related')""".trimIndent()
            )
            db.execSQL("DROP TABLE links")
            db.execSQL("ALTER TABLE links_v2 RENAME TO links")
        }
        if (oldVersion < 3) createIndexes(db)
    }

    private fun createIndexes(db: SQLiteDatabase) {
        // Recent-note listing and graph traversal should remain responsive as the
        // local knowledge base grows. The primary key already indexes source_id.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_links_target_id ON links(target_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_links_relation ON links(relation)")
    }

    fun saveNote(title: String, body: String, id: Long? = null): Long {
        require(title.isNotBlank()) { "Judul catatan tidak boleh kosong." }
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title.trim())
            put("body", body)
            put("updated_at", now)
        }
        return writableDatabase.let { db ->
            if (id == null) db.insertOrThrow("notes", null, values)
            else {
                val changed = db.update("notes", values, "id = ?", arrayOf(id.toString()))
                check(changed > 0) { "Catatan yang akan diperbarui sudah tidak tersedia." }
                id
            }
        }
    }

    fun searchNotes(query: String = ""): List<KnowledgeNote> {
        val q = query.trim()
        // Treat user input literally: '%' and '_' are search characters, not
        // implicit SQL wildcards. Backslash is escaped before LIKE matching.
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val cursor = readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "updated_at"),
            if (q.isEmpty()) null else "(title LIKE ? ESCAPE '\\' OR body LIKE ? ESCAPE '\\')",
            if (q.isEmpty()) null else arrayOf("%$escaped%", "%$escaped%"),
            null, null, "updated_at DESC"
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(
                    KnowledgeNote(it.getLong(0), it.getString(1), it.getString(2), it.getLong(3))
                )
            }
        }
    }

    fun countNotes(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM notes", null)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun countLinks(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM links", null)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun deleteNote(noteId: Long): Boolean {
        return writableDatabase.delete("notes", "id = ?", arrayOf(noteId.toString())) > 0
    }

    fun unlinkNotes(sourceId: Long, targetId: Long, relation: String = "related"): Int {
        val first = minOf(sourceId, targetId)
        val second = maxOf(sourceId, targetId)
        return writableDatabase.delete(
            "links",
            "source_id = ? AND target_id = ? AND relation = ?",
            arrayOf(first.toString(), second.toString(), relation.trim().ifBlank { "related" })
        )
    }

    fun linkNotes(sourceId: Long, targetId: Long, relation: String = "related") {
        require(sourceId > 0 && targetId > 0) { "ID node tidak valid." }
        require(sourceId != targetId) { "Node tidak dapat dihubungkan ke dirinya sendiri." }
        val normalizedRelation = relation.trim().ifBlank { "related" }
        require(normalizedRelation.length <= 128) { "Nama relasi maksimal 128 karakter." }
        // Relations are undirected in the current graph model. Canonical ordering
        // prevents A-B and B-A from becoming duplicate edges.
        val first = minOf(sourceId, targetId)
        val second = maxOf(sourceId, targetId)
        val values = ContentValues().apply {
            put("source_id", first)
            put("target_id", second)
            put("relation", normalizedRelation)
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Check endpoints explicitly so callers get a clear domain error instead
            // of a low-level SQLite foreign-key exception.
            val existing = db.rawQuery(
                "SELECT COUNT(*) FROM notes WHERE id IN (?, ?)",
                arrayOf(first.toString(), second.toString())
            ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 2 }
            check(existing) { "Catatan sumber atau target sudah tidak tersedia." }
            db.insertWithOnConflict("links", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun relatedNotes(noteId: Long): List<KnowledgeNote> {
        val cursor = readableDatabase.rawQuery(
            """SELECT DISTINCT n.id, n.title, n.body, n.updated_at
               FROM notes n JOIN links l ON
               (n.id = l.target_id AND l.source_id = ?) OR
               (n.id = l.source_id AND l.target_id = ?)
               WHERE n.id != ?
               ORDER BY n.updated_at DESC""".trimIndent(),
            arrayOf(noteId.toString(), noteId.toString(), noteId.toString())
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(
                    KnowledgeNote(it.getLong(0), it.getString(1), it.getString(2), it.getLong(3))
                )
            }
        }
    }

    /**
     * Portable, dependency-free JSON snapshot of notes and undirected relations.
     * Returns data only; the caller chooses Android's document picker or another
     * explicit destination for writing the export.
     */
    fun exportJson(): String {
        val notes = searchNotes("").sortedBy { it.id }
        val links = readableDatabase.rawQuery(
            "SELECT source_id, target_id, relation FROM links ORDER BY source_id, target_id, relation",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Triple(cursor.getLong(0), cursor.getLong(1), cursor.getString(2)))
                }
            }
        }
        fun quote(value: String): String = buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
                }
            }
            append('"')
        }
        return buildString {
            append("{\n  \"format\": \"lynos-mnemos-export-v1\",\n  \"notes\": [")
            notes.forEachIndexed { index, note ->
                append(if (index == 0) "\n" else ",\n")
                append("    {\"id\": ").append(note.id)
                append(", \"title\": ").append(quote(note.title))
                append(", \"body\": ").append(quote(note.body))
                append(", \"updatedAt\": ").append(note.updatedAt).append('}')
            }
            if (notes.isNotEmpty()) append('\n')
            append("  ],\n  \"links\": [")
            links.forEachIndexed { index, link ->
                append(if (index == 0) "\n" else ",\n")
                append("    {\"sourceId\": ").append(link.first)
                append(", \"targetId\": ").append(link.second)
                append(", \"relation\": ").append(quote(link.third)).append('}')
            }
            if (links.isNotEmpty()) append('\n')
            append("  ]\n}\n")
        }
    }

    /**
     * Human-readable Markdown snapshot. Link IDs are retained so repeated titles
     * remain distinguishable and the relation list can be reconciled with JSON.
     */
    fun exportMarkdown(): String {
        val notes = searchNotes("").sortedBy { it.id }
        val links = readableDatabase.rawQuery(
            "SELECT source_id, target_id, relation FROM links ORDER BY source_id, target_id, relation",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Triple(cursor.getLong(0), cursor.getLong(1), cursor.getString(2)))
                }
            }
        }
        fun escapeCell(value: String) = value.replace("\\", "\\\\")
            .replace("|", "\\|").replace("\r", "").replace("\n", "<br>")
        return buildString {
            append("# Mnemos export\n\n")
            append("Format: `lynos-mnemos-export-v1`  \n")
            append("Exported at: ").append(System.currentTimeMillis()).append(" (Unix milliseconds)\n\n")
            append("## Notes\n\n")
            if (notes.isEmpty()) append("_No notes._\n")
            notes.forEach { note ->
                append("### ").append(note.title.replace("\r", "").replace("\n", " ")).append(" (ID ")
                    .append(note.id).append(")\n\n")
                append(note.body.ifBlank { "_Empty note._" }).append("\n\n")
                append("_Updated at: ").append(note.updatedAt).append("_\n\n")
            }
            append("## Relations\n\n")
            if (links.isEmpty()) append("_No relations._\n")
            else {
                append("| Source ID | Relation | Target ID |\n|---:|---|---:|\n")
                links.forEach { link ->
                    append("| ").append(link.first).append(" | ")
                        .append(escapeCell(link.third)).append(" | ")
                        .append(link.second).append(" |\n")
                }
            }
        }
    }

}
