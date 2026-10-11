package com.decisionpro.app.data.local

import androidx.room.migration.Migration

/**
 * NEVER fallbackToDestructiveMigration — user history must survive upgrades.
 * Template for the next change:
 *
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE decisions ADD COLUMN newField TEXT DEFAULT NULL")
 *     }
 * }
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf(
        // MIGRATION_1_2,  ← register as versions advance
    )
}
