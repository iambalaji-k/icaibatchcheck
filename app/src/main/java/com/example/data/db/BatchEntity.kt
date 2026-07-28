package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey val id: String, // composite key like "region_pou_course_batchname"
    val batchName: String,
    val totalSeats: Int,
    val availableSeats: Int,
    val dates: String,
    val timings: String,
    val venue: String,
    val fee: String,
    val regionName: String,
    val pouName: String,
    val courseName: String,
    val lastCheckedTimestamp: Long,
    val isOpen: Boolean
)
