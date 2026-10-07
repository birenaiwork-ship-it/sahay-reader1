package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audiobook_segments",
    indices = [
        Index(value = ["audiobookId", "segmentIndex"]),
        Index(value = ["audiobookId"])
    ]
)
data class AudiobookSegmentEntity(
    @PrimaryKey
    val id: String,
    val audiobookId: String,
    val segmentIndex: Int,
    val pageNumber: Int,
    val readingUnitId: String,
    val textSpoken: String,
    val audioFilePath: String,
    val durationMs: Long = 0L,
    val language: String = "en"
)
