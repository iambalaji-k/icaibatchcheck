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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Query("DELETE FROM batches")
    suspend fun clearAllBatches()

    @Query("DELETE FROM batches WHERE LOWER(pouName) = LOWER(:pouName) AND (LOWER(courseName) = LOWER(:courseName) OR LOWER(courseName) LIKE LOWER('%' || :courseName || '%') OR LOWER(:courseName) LIKE LOWER('%' || courseName || '%'))")
    suspend fun deleteBatchesByPouAndCourse(pouName: String, courseName: String)
}
