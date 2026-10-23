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
}
