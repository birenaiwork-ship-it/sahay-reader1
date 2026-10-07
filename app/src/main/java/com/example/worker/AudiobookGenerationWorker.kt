package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.SahayaApplication
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AudiobookEntity
import com.example.data.local.entity.AudiobookSegmentEntity
import com.example.data.local.entity.TranslationEntity
import com.example.data.remote.GoogleCloudTtsProvider
import com.example.data.remote.TtsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class AudiobookGenerationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "audiobook_generation_channel"
    private val notificationId = 1001

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val audiobookId = inputData.getString("AUDIOBOOK_ID") ?: return@withContext Result.failure()
        val bookId = inputData.getString("BOOK_ID") ?: return@withContext Result.failure()
        val bookTitle = inputData.getString("BOOK_TITLE") ?: "Audiobook"
        val languageMode = inputData.getString("LANGUAGE_MODE") ?: "ENGLISH"

        createNotificationChannel()

        val db = AppDatabase.getInstance(context)
        val app = context.applicationContext as? SahayaApplication
        val translationRepo = app?.translationRepository
        val prefsRepo = app?.preferencesRepository
        val cloudTts = if (prefsRepo != null) GoogleCloudTtsProvider(prefsRepo) else null

        val readingUnits = db.readingUnitDao().getAllReadingUnitsForBook(bookId)
        if (readingUnits.isEmpty()) {
            db.audiobookDao().insertOrUpdateAudiobook(
                AudiobookEntity(
                    id = audiobookId,
                    bookId = bookId,
                    title = bookTitle,
                    languageMode = languageMode,
                    status = AudiobookEntity.STATUS_FAILED,
                    errorMessage = "No reading units found for this book"
                )
            )
            return@withContext Result.failure()
        }

        // Output directory
        val audiobooksDir = File(context.filesDir, "audiobooks").apply { mkdirs() }
        val tempSegmentsDir = File(context.cacheDir, "segments_$audiobookId").apply { mkdirs() }
        val finalAudiobookFile = File(audiobooksDir, "book_${bookId}_$audiobookId.mp3")

        val totalUnits = readingUnits.size
        val totalPages = readingUnits.maxOfOrNull { it.pageNumber } ?: 1

        db.audiobookDao().insertOrUpdateAudiobook(
            AudiobookEntity(
                id = audiobookId,
                bookId = bookId,
                title = bookTitle,
                languageMode = languageMode,
                status = AudiobookEntity.STATUS_PROCESSING,
                progressPercent = 0,
                currentProcessingPage = 1,
                totalPages = totalPages,
                currentReadingUnitIndex = 0,
                totalReadingUnits = totalUnits,
                filePath = finalAudiobookFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )

        updateNotification("Starting audiobook generation…", 0)

        val segmentFiles = mutableListOf<File>()
        val segmentEntities = mutableListOf<AudiobookSegmentEntity>()

        try {
            for ((index, unit) in readingUnits.withIndex()) {
                if (isStopped) {
                    tempSegmentsDir.deleteRecursively()
                    return@withContext Result.failure()
                }

                val unitNumber = index + 1
                val progressPercent = ((unitNumber.toFloat() / totalUnits.toFloat()) * 90).toInt()

                val notificationMessage = "Page ${unit.pageNumber} of $totalPages (Unit $unitNumber of $totalUnits)"
                updateNotification(notificationMessage, progressPercent)

                db.audiobookDao().updateProgress(
                    audiobookId = audiobookId,
                    progress = progressPercent,
                    page = unit.pageNumber,
                    unitIndex = unitNumber,
                    status = AudiobookEntity.STATUS_PROCESSING
                )

                // Determine text to synthesize based on mode
                val textToSpeak: String = when (languageMode) {
                    "ODIA" -> {
                        // Ensure translated
                        var trans = db.translationDao().getTranslation(unit.id)
                        if (trans == null || trans.status != TranslationEntity.STATUS_TRANSLATED || trans.odiaText.isBlank()) {
                            translationRepo?.translateReadingUnit(unit.id, bookId, unit.englishText)
                            trans = db.translationDao().getTranslation(unit.id)
                        }
                        if (trans?.odiaText?.isNotBlank() == true) trans.odiaText else unit.englishText
                    }
                    "BILINGUAL" -> {
                        // English first, then Odia
                        var trans = db.translationDao().getTranslation(unit.id)
                        if (trans == null || trans.status != TranslationEntity.STATUS_TRANSLATED || trans.odiaText.isBlank()) {
                            translationRepo?.translateReadingUnit(unit.id, bookId, unit.englishText)
                            trans = db.translationDao().getTranslation(unit.id)
                        }
                        val odiaPart = trans?.odiaText?.ifBlank { null } ?: ""
                        if (odiaPart.isNotBlank()) {
                            "${unit.englishText}. $odiaPart"
                        } else {
                            unit.englishText
                        }
                    }
                    else -> unit.englishText
                }

                // Generate audio segment
                val segmentFile = File(tempSegmentsDir, "seg_${index}_${unit.id}.mp3")
                val langCode = if (languageMode == "ODIA") "or-IN" else "en-US"

                // Try cloud TTS first, or local synthesis
                var generatedSuccessfully = false
                if (cloudTts != null) {
                    val res = cloudTts.synthesizeToFile(textToSpeak, langCode, segmentFile)
                    if (res is TtsResult.Success && segmentFile.exists() && segmentFile.length() > 0) {
                        generatedSuccessfully = true
                    }
                }

                if (!generatedSuccessfully) {
                    // Fallback to local synthesizer
                    val localDone = kotlinx.coroutines.CompletableDeferred<Boolean>()
                    app?.ttsManager?.synthesizeToFile(
                        text = textToSpeak,
                        language = if (languageMode == "ODIA") "or" else "en",
                        speedRate = 1.0f,
                        outputFile = segmentFile
                    ) { success ->
                        localDone.complete(success)
                    }
                    // Wait up to 5 seconds per sentence
                    kotlinx.coroutines.withTimeoutOrNull(5000) {
                        localDone.await()
                    }
                    if (segmentFile.exists() && segmentFile.length() > 0) {
                        generatedSuccessfully = true
                    }
                }

                if (segmentFile.exists() && segmentFile.length() > 0) {
                    segmentFiles.add(segmentFile)
                    segmentEntities.add(
                        AudiobookSegmentEntity(
                            id = UUID.randomUUID().toString(),
                            audiobookId = audiobookId,
                            segmentIndex = index,
                            pageNumber = unit.pageNumber,
                            readingUnitId = unit.id,
                            textSpoken = textToSpeak,
                            audioFilePath = segmentFile.absolutePath,
                            durationMs = (textToSpeak.length * 65L).coerceAtLeast(1000L),
                            language = langCode
                        )
                    )
                }

                // Small throttle to be gentle with resources
                delay(30)
            }

            // Combine segment files into final audio package
            if (segmentFiles.isEmpty()) {
                throw Exception("No audio segments could be synthesized. For Odia speech, install the Odia voice in Android TTS settings or configure Cloud TTS on the backend.")
            }
            updateNotification("Finalizing audiobook file…", 95)
            FileOutputStream(finalAudiobookFile).use { out ->
                for (seg in segmentFiles) {
                    if (seg.exists()) {
                        FileInputStream(seg).use { inStream ->
                            inStream.copyTo(out)
                        }
                    }
                }
            }

            val finalSize = finalAudiobookFile.length()
            val totalDurationMs = segmentEntities.sumOf { it.durationMs }

            db.audiobookDao().insertSegments(segmentEntities)
            db.audiobookDao().insertOrUpdateAudiobook(
                AudiobookEntity(
                    id = audiobookId,
                    bookId = bookId,
                    title = bookTitle,
                    languageMode = languageMode,
                    status = AudiobookEntity.STATUS_COMPLETED,
                    progressPercent = 100,
                    currentProcessingPage = totalPages,
                    totalPages = totalPages,
                    currentReadingUnitIndex = totalUnits,
                    totalReadingUnits = totalUnits,
                    filePath = finalAudiobookFile.absolutePath,
                    fileFormat = "MP3",
                    fileSizeBytes = finalSize,
                    totalDurationMs = totalDurationMs,
                    createdAt = System.currentTimeMillis()
                )
            )

            showCompletedNotification(bookTitle)
            tempSegmentsDir.deleteRecursively()
            Result.success()
        } catch (e: Exception) {
            db.audiobookDao().insertOrUpdateAudiobook(
                AudiobookEntity(
                    id = audiobookId,
                    bookId = bookId,
                    title = bookTitle,
                    languageMode = languageMode,
                    status = AudiobookEntity.STATUS_FAILED,
                    errorMessage = "Generation error: ${e.localizedMessage}"
                )
            )
            updateNotification("Audiobook generation failed: ${e.localizedMessage}", 0)
            tempSegmentsDir.deleteRecursively()
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Audiobook Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of audiobook generation"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(text: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Sahaya Reader — Audiobook")
            .setContentText(text)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showCompletedNotification(bookTitle: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Audiobook is ready!")
            .setContentText("Finished generating audiobook for: $bookTitle")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}
