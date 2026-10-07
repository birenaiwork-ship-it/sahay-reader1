package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    indices = [
        Index(value = ["bookId", "pageNumber"], unique = true),
        Index(value = ["bookId"])
    ]
)
data class PageEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val isOcr: Boolean = false,
    val ocrConfidence: Float = 1.0f,
    val headerText: String? = null,
    val footerText: String? = null,
    val rawText: String = ""
)
