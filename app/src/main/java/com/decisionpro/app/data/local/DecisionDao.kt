package com.decisionpro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.decisionpro.app.data.model.DecisionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {

    @Query("SELECT * FROM decisions WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DecisionRecord>>

    @Query("SELECT * FROM decisions WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<DecisionRecord>

    @Query("SELECT * FROM decisions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DecisionRecord?

    @Insert
    suspend fun insert(record: DecisionRecord): Long

    @Insert
    suspend fun insertAll(records: List<DecisionRecord>)

    @Update
    suspend fun update(record: DecisionRecord)

    @Query("UPDATE decisions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE decisions SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM decisions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM decisions WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Query("DELETE FROM decisions WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeSoftDeletedOlderThan(cutoff: Long)
}
