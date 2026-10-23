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
class MnemosDatabase(context: Context) : SQLiteOpenHelper(context, "mnemos.db", null, 1) {
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versioned migrations will be added as the schema evolves.
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
                if (changed == 0) db.insertOrThrow("notes", null, values) else id
            }
        }
    }

    fun searchNotes(query: String = ""): List<KnowledgeNote> {
        val q = query.trim()
        val cursor = readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "updated_at"),
            if (q.isEmpty()) null else "title LIKE ? OR body LIKE ?",
            if (q.isEmpty()) null else arrayOf("%$q%", "%$q%"),
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

    fun deleteNote(noteId: Long): Boolean {
        return writableDatabase.delete("notes", "id = ?", arrayOf(noteId.toString())) > 0
    }

    fun unlinkNotes(sourceId: Long, targetId: Long, relation: String = "related"): Int {
        return writableDatabase.delete(
            "links",
            "((source_id = ? AND target_id = ?) OR (source_id = ? AND target_id = ?)) AND relation = ?",
            arrayOf(sourceId.toString(), targetId.toString(), targetId.toString(), sourceId.toString(), relation)
        )
    }

    fun linkNotes(sourceId: Long, targetId: Long, relation: String = "related") {
        require(sourceId != targetId) { "Node tidak dapat dihubungkan ke dirinya sendiri." }
        val values = ContentValues().apply {
            put("source_id", sourceId)
            put("target_id", targetId)
            put("relation", relation.ifBlank { "related" })
        }
        writableDatabase.insertWithOnConflict("links", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun relatedNotes(noteId: Long): List<KnowledgeNote> {
        val cursor = readableDatabase.rawQuery(
            """SELECT n.id, n.title, n.body, n.updated_at
               FROM notes n JOIN links l ON
               (n.id = l.target_id AND l.source_id = ?) OR
               (n.id = l.source_id AND l.target_id = ?)
               ORDER BY n.updated_at DESC""".trimIndent(),
            arrayOf(noteId.toString(), noteId.toString())
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
