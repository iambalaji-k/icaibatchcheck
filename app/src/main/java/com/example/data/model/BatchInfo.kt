package com.example.data.model

data class BatchInfo(
    val batchName: String,
    val totalSeats: Int,
    val availableSeats: Int,
    val dates: String = "",
    val timings: String = "",
    val venue: String = "",
    val fee: String = "",
    val statusText: String = "",
    val regionName: String = "",
    val pouName: String = "",
    val courseName: String = ""
) {
    val isOpen: Boolean get() = availableSeats > 0
}

data class DropdownOption(
    val value: String,
    val text: String
)

sealed class ScraperResult<out T> {
    data class Success<out T>(val data: T) : ScraperResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ScraperResult<Nothing>()
}
