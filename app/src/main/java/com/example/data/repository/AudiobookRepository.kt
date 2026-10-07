package com.example.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.local.dao.AudiobookDao
import com.example.data.local.entity.AudiobookEntity
import com.example.data.local.entity.AudiobookSegmentEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class AudiobookRepository(
    private val context: Context,
    private val audiobookDao: AudiobookDao
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun getAllAudiobooksFlow(): Flow<List<AudiobookEntity>> =
        audiobookDao.getAllAudiobooksFlow()

    fun getAudiobookByIdFlow(audiobookId: String): Flow<AudiobookEntity?> =
        audiobookDao.getAudiobookByIdFlow(audiobookId)

    suspend fun getAudiobookById(audiobookId: String): AudiobookEntity? =
        audiobookDao.getAudiobookById(audiobookId)

    suspend fun getLatestAudiobookForBook(bookId: String): AudiobookEntity? =
        audiobookDao.getLatestAudiobookForBook(bookId)

    suspend fun createAndStartAudiobookGeneration(
        bookId: String,
        bookTitle: String,
        languageMode: String,
        totalPages: Int,
        totalUnits: Int
    ): String {
        val audiobookId = UUID.randomUUID().toString()
        val entity = AudiobookEntity(
            id = audiobookId,
            bookId = bookId,
            title = bookTitle,
            languageMode = languageMode,
            status = AudiobookEntity.STATUS_QUEUED,
            progressPercent = 0,
            currentProcessingPage = 0,
            totalPages = totalPages,
            currentReadingUnitIndex = 0,
            totalReadingUnits = totalUnits,
            fileFormat = "MP3",
            createdAt = System.currentTimeMillis()
        )
        audiobookDao.insertOrUpdateAudiobook(entity)

        // Queue WorkManager task
        val inputData = Data.Builder()
            .putString("AUDIOBOOK_ID", audiobookId)
            .putString("BOOK_ID", bookId)
            .putString("BOOK_TITLE", bookTitle)
            .putString("LANGUAGE_MODE", languageMode)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<com.example.worker.AudiobookGenerationWorker>()
            .setInputData(inputData)
            .addTag("audiobook_$audiobookId")
            .build()

        workManager.enqueueUniqueWork(
            "audiobook_work_$audiobookId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return audiobookId
    }

    suspend fun updatePlaybackPosition(audiobookId: String, positionMs: Long) {
        audiobookDao.updatePlaybackPosition(audiobookId, positionMs)
    }

    suspend fun deleteAudiobook(audiobookId: String) {
        val entity = audiobookDao.getAudiobookById(audiobookId)
        entity?.filePath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }
        audiobookDao.deleteSegmentsForAudiobook(audiobookId)
        audiobookDao.deleteAudiobookById(audiobookId)
    }

    suspend fun getSegments(audiobookId: String): List<AudiobookSegmentEntity> =
        audiobookDao.getSegmentsForAudiobook(audiobookId)
}
