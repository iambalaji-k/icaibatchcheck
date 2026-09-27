package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY isOpen DESC, availableSeats DESC, batchName ASC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: String): BatchEntity?

    @Query("SELECT * FROM batches WHERE id IN (:ids)")
    suspend fun getBatchesByIds(ids: List<String>): List<BatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Query("DELETE FROM batches")
    suspend fun clearAllBatches()

    // Prune rows of one target that the latest scrape no longer reports.
    @Query(
        "DELETE FROM batches " +
            "WHERE regionName = :regionText AND pouName = :pouText AND courseName = :courseText " +
            "AND id NOT IN (:keepIds)"
    )
    suspend fun deleteStaleBatches(regionText: String, pouText: String, courseText: String, keepIds: List<String>)

    @Query(
        "DELETE FROM batches " +
            "WHERE regionName = :regionText AND pouName = :pouText AND courseName = :courseText"
    )
    suspend fun deleteBatchesByTarget(regionText: String, pouText: String, courseText: String)
}
