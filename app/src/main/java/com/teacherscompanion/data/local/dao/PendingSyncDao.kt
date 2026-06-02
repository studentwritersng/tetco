package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.PendingSyncEntity

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync WHERE entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getPendingByType(entityType: String): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()

    @Query("DELETE FROM pending_sync WHERE entityType = :entityType")
    suspend fun deleteByType(entityType: String)
}
