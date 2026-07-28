package com.example.data.model

import java.util.UUID

data class BatchTarget(
    val id: String = UUID.randomUUID().toString(),
    val regionValue: String = "4",
    val regionText: String = "Southern",
    val pouValue: String = "3",
    val pouText: String = "Chennai",
    val courseValue: String = "48",
    val courseText: String = "AICITSS - Advanced Information Technology",
    val isEnabled: Boolean = true
)
