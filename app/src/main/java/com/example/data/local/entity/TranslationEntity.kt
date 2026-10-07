package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translations",
    indices = [
        Index(value = ["bookId"])
    ]
)
data class TranslationEntity(
    @PrimaryKey
    val readingUnitId: String,
    val bookId: String,
    val sourceText: String,
    val odiaText: String = "",
    val status: String = STATUS_NOT_TRANSLATED,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_NOT_TRANSLATED = "NOT_TRANSLATED"
        const val STATUS_TRANSLATING = "TRANSLATING"
        const val STATUS_TRANSLATED = "TRANSLATED"
        const val STATUS_FAILED = "FAILED"
    }
}
