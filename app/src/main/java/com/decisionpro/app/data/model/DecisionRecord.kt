package com.decisionpro.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decisions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["type", "timestamp"])
    ]
)
data class DecisionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val choice: String,
    val type: String,
    val confidence: Int? = null,
    val decisionTimeMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
