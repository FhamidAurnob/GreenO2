package com.example.greeno2.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.greeno2.domain.CarbonCalculator
import java.util.UUID

/**
 * Local SQLite store. Everything goes through [GreenRepository], so this can be swapped for a
 * remote backend (Firebase, Supabase, a REST API) later without touching the UI.
 */
class GreenDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                is_demo INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                mode TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                distance_m REAL NOT NULL,
                emitted_kg REAL NOT NULL,
                saved_kg REAL NOT NULL,
                credits INTEGER NOT NULL,
                flagged INTEGER NOT NULL DEFAULT 0,
                note TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_trips_user ON trips(user_id, start_time)")
        db.execSQL(
            """
            CREATE TABLE redemptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                item_id TEXT NOT NULL,
                title TEXT NOT NULL,
                cost INTEGER NOT NULL,
                code TEXT NOT NULL,
                time INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seedDemoCommunity(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No migrations yet (v1). Add ALTER TABLE steps here when the schema changes.
        db.execSQL("DROP TABLE IF EXISTS redemptions")
        db.execSQL("DROP TABLE IF EXISTS trips")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    /**
     * Seeds a few clearly-labelled demo riders so the leaderboard is not empty on a fresh
     * install. They cannot log in (random password) and are shown with a "demo" tag.
     */
    private fun seedDemoCommunity(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val demo = listOf(
            "Ayesha" to listOf(TransportMode.BIKE to 6200.0, TransportMode.BIKE to 5800.0, TransportMode.BUS to 12000.0),
            "Liam" to listOf(TransportMode.WALK to 2400.0, TransportMode.TRAIN to 18000.0),
            "Sofia" to listOf(TransportMode.E_SCOOTER to 4100.0, TransportMode.E_BIKE to 9000.0, TransportMode.WALK to 1800.0),
            "Kenji" to listOf(TransportMode.BUS to 7500.0),
        )
        demo.forEachIndexed { i, (name, trips) ->
            val userId = db.insert("users", null, ContentValues().apply {
                put("name", name)
                put("email", "demo${i + 1}@greenmiles.demo")
                put("password_hash", UUID.randomUUID().toString())
                put("password_salt", UUID.randomUUID().toString())
                put("is_demo", 1)
                put("created_at", now - 30 * day)
            })
            trips.forEachIndexed { j, (mode, meters) ->
                val calc = CarbonCalculator.calculate(mode, meters)
                val start = now - (j + 1) * day - i * 3_600_000L
                val speedKmh = (mode.maxAvgSpeedKmh * 0.5).coerceAtLeast(4.0)
                val durationMs = (meters / 1000.0 / speedKmh * 3_600_000).toLong()
                db.insert("trips", null, ContentValues().apply {
                    put("user_id", userId)
                    put("mode", mode.name)
                    put("start_time", start)
                    put("end_time", start + durationMs)
                    put("distance_m", meters)
                    put("emitted_kg", calc.emittedKg)
                    put("saved_kg", calc.savedKg)
                    put("credits", calc.credits)
                    put("flagged", 0)
                })
            }
        }
    }

    companion object {
        const val DB_NAME = "green_miles.db"
        const val DB_VERSION = 1
    }
}
