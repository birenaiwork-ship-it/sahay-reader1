package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_units",
    indices = [
        Index(value = ["bookId", "pageNumber", "unitIndexInPage"], unique = true),
        Index(value = ["bookId", "pageNumber"]),
        Index(value = ["bookId"])
    ]
)
data class ReadingUnitEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val paragraphIndex: Int,
    val unitIndexInPage: Int,
    val englishText: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0
)
