package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {
    @Query("SELECT * FROM check_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<CheckLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CheckLogEntity)

    @Query("DELETE FROM check_logs")
    suspend fun clearLogs()
}
