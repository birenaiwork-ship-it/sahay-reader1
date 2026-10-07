package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String = "Unknown",
    val pageCount: Int = 1,
    val currentPage: Int = 1,
    val currentReadingUnitId: String? = null,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isSampleBook: Boolean = false,
    val sourceUri: String? = null,
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
