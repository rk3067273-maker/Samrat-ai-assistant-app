package com.samrat.aiassistant.ai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LongTermMemory(context: Context) : SQLiteOpenHelper(context, "samrat_memory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                topic TEXT,
                content TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS memories")
        onCreate(db)
    }

    /**
     * Store new memory or fact about SAMRAT
     */
    fun saveMemory(topic: String, content: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("topic", topic)
            put("content", content)
        }
        db.insert("memories", null, values)
    }

    /**
     * Retrieve relevant memories formatted for system prompt injection
     */
    fun getMemoriesSummary(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT topic, content FROM memories ORDER BY id DESC LIMIT 15", null)
        val sb = StringBuilder()
        while (cursor.moveToNext()) {
            val topic = cursor.getString(0)
            val content = cursor.getString(1)
            sb.append("- [$topic]: $content\n")
        }
        cursor.close()
        return sb.toString()
    }
}
