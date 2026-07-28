package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_logs")
data class CheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "NO_SEATS", "ERROR", "ALERT_TRIGGERED"
    val message: String,
    val regionName: String,
    val pouName: String,
    val courseName: String,
    val openBatchesCount: Int
)
