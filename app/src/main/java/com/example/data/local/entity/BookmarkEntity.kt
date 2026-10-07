package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["bookId"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val readingUnitId: String,
    val readingUnitSnippet: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
