package com.urunkarpm.drawer.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `todo` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `note` TEXT,
                `place_name` TEXT,
                `latitude` REAL,
                `longitude` REAL,
                `radius_m` REAL NOT NULL,
                `deadline_utc` INTEGER,
                `deadline_zone_id` TEXT,
                `priority` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `snoozed_until` INTEGER,
                `completed_at` INTEGER,
                `stale_since` INTEGER,
                `nudge_count` INTEGER NOT NULL,
                `last_nudge_at` INTEGER,
                `last_location_alert_at` INTEGER,
                `repeat_rule` TEXT,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_place` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `radius_m` REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recent_search` (
                `query` TEXT PRIMARY KEY NOT NULL,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `event_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `time` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `text` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
