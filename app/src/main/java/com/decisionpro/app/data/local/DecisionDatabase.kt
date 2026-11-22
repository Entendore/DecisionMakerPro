package com.decisionpro.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decisionpro.app.data.model.DecisionRecord

/**
 * v1 = full final schema (indices + soft-delete). Schema JSON exports to
 * app/schemas/ — COMMIT these. When changing an @Entity:
 *   bump version → rebuild → write Migration in Migrations.kt →
 *   add MigrationTest → commit schema+migration+test in the SAME PR.
 */
@Database(
    entities = [DecisionRecord::class],
    version = 1,
    exportSchema = true
)
abstract class DecisionDatabase : RoomDatabase() {
    abstract fun decisionDao(): DecisionDao
    companion object { const val DB_NAME = "decision_pro.db" }
}
