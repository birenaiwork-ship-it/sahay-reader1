package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.GoogleCloudTranslationProvider
import com.example.data.repository.AudiobookRepository
import com.example.data.repository.BookmarkRepository
import com.example.data.repository.BookRepository
import com.example.data.repository.TranslationRepository
import com.example.pdf.PdfTextExtractor
import com.example.sample.SampleBookProvider
import com.example.speech.AndroidTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SahayaApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var translationRepository: TranslationRepository
        private set

    lateinit var bookmarkRepository: BookmarkRepository
        private set

    lateinit var bookRepository: BookRepository
        private set

    lateinit var audiobookRepository: AudiobookRepository
        private set

    lateinit var ttsManager: AndroidTtsManager
        private set

    lateinit var pdfTextExtractor: PdfTextExtractor
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)

        val translationProvider = GoogleCloudTranslationProvider(preferencesRepository)
        translationRepository = TranslationRepository(
            translationDao = database.translationDao(),
            translationProvider = translationProvider
        )

        bookmarkRepository = BookmarkRepository(database.bookmarkDao())

        bookRepository = BookRepository(
            bookDao = database.bookDao(),
            pageDao = database.pageDao(),
            readingUnitDao = database.readingUnitDao(),
            translationDao = database.translationDao(),
            bookmarkDao = database.bookmarkDao(),
            audiobookDao = database.audiobookDao()
        )

        audiobookRepository = AudiobookRepository(
            context = this,
            audiobookDao = database.audiobookDao()
        )

        ttsManager = AndroidTtsManager(this)
        pdfTextExtractor = PdfTextExtractor(this)

        // Preload sample textbook asynchronously so user can test immediately
        applicationScope.launch {
            SampleBookProvider.seedSampleBookIfNotPresent(
                context = this@SahayaApplication,
                bookRepository = bookRepository,
                translationRepository = translationRepository
            )
        }
    }

    override fun onTerminate() {
        ttsManager.shutdown()
        super.onTerminate()
    }
}
