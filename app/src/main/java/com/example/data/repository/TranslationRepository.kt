package com.example.data.repository

import com.example.data.local.dao.TranslationDao
import com.example.data.local.entity.TranslationEntity
import com.example.data.remote.TranslationProvider
import com.example.data.remote.TranslationResult
import kotlinx.coroutines.flow.Flow

class TranslationRepository(
    private val translationDao: TranslationDao,
    private val translationProvider: TranslationProvider
) {

    fun getTranslationFlow(unitId: String): Flow<TranslationEntity?> {
        return translationDao.getTranslationFlow(unitId)
    }

    suspend fun getCachedTranslation(unitId: String): TranslationEntity? {
        return translationDao.getTranslation(unitId)
    }

    suspend fun translateReadingUnit(
        unitId: String,
        bookId: String,
        sourceText: String,
        forceRefresh: Boolean = false
    ): Result<String> {
        if (sourceText.isBlank()) {
            return Result.success("")
        }

        val cached = translationDao.getTranslation(unitId)
        if (!forceRefresh && cached != null && cached.status == TranslationEntity.STATUS_TRANSLATED && cached.odiaText.isNotBlank()) {
            return Result.success(cached.odiaText)
        }

        // Mark as TRANSLATING
        val initialEntity = TranslationEntity(
            readingUnitId = unitId,
            bookId = bookId,
            sourceText = sourceText,
            odiaText = cached?.odiaText ?: "",
            status = TranslationEntity.STATUS_TRANSLATING,
            updatedAt = System.currentTimeMillis()
        )
        translationDao.insertOrUpdateTranslation(initialEntity)

        return when (val result = translationProvider.translate(sourceText, "en", "or")) {
            is TranslationResult.Success -> {
                val updated = TranslationEntity(
                    readingUnitId = unitId,
                    bookId = bookId,
                    sourceText = sourceText,
                    odiaText = result.translatedText,
                    status = TranslationEntity.STATUS_TRANSLATED,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
                translationDao.insertOrUpdateTranslation(updated)
                Result.success(result.translatedText)
            }
            is TranslationResult.Error -> {
                val failed = TranslationEntity(
                    readingUnitId = unitId,
                    bookId = bookId,
                    sourceText = sourceText,
                    odiaText = cached?.odiaText ?: "",
                    status = TranslationEntity.STATUS_FAILED,
                    errorMessage = result.message,
                    updatedAt = System.currentTimeMillis()
                )
                translationDao.insertOrUpdateTranslation(failed)
                Result.failure(Exception(result.message, result.throwable))
            }
        }
    }

    suspend fun clearTranslationsForBook(bookId: String) {
        translationDao.deleteTranslationsForBook(bookId)
    }

    suspend fun clearAllTranslations() {
        translationDao.clearAllTranslations()
    }
}
