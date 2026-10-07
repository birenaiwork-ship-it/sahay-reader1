package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audiobooks",
    indices = [
        Index(value = ["bookId"])
    ]
)
data class AudiobookEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val title: String,
    val languageMode: String, // ENGLISH, ODIA, BILINGUAL
    val status: String = STATUS_QUEUED,
    val progressPercent: Int = 0,
    val currentProcessingPage: Int = 0,
    val totalPages: Int = 0,
    val currentReadingUnitIndex: Int = 0,
    val totalReadingUnits: Int = 0,
    val filePath: String? = null,
    val fileFormat: String = "MP3",
    val fileSizeBytes: Long = 0L,
    val totalDurationMs: Long = 0L,
    val lastPlaybackPositionMs: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
