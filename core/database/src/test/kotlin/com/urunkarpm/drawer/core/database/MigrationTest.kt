package com.urunkarpm.drawer.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationTest {

    @Test
    fun migration_4_to_5_executesRequiredDDL() {
        val db: SupportSQLiteDatabase = mockk(relaxed = true)

        assertEquals(4, MIGRATION_4_5.startVersion)
        assertEquals(5, MIGRATION_4_5.endVersion)

        MIGRATION_4_5.migrate(db)

        verify {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `todo`") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `saved_place`") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `recent_search`") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `event_log`") })
        }
    }
}
