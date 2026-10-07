package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.ReadingUnitEntity
import com.example.data.local.entity.TranslationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sahaya Reader", appName)
    }

    @Test
    fun testBookDao_insertAndQuery() = runBlocking {
        val book = BookEntity(
            id = "test_book_1",
            title = "Computer Architecture",
            author = "Tanenbaum",
            pageCount = 120,
            currentPage = 5,
            currentReadingUnitId = "test_book_1_p5_u2"
        )
        db.bookDao().insertOrUpdateBook(book)

        val retrieved = db.bookDao().getBookById("test_book_1")
        assertNotNull(retrieved)
        assertEquals("Computer Architecture", retrieved?.title)
        assertEquals(5, retrieved?.currentPage)

        db.bookDao().updateReadingPosition("test_book_1", 10, "test_book_1_p10_u1")
        val updated = db.bookDao().getBookById("test_book_1")
        assertEquals(10, updated?.currentPage)
    }

    @Test
    fun testTranslationDao_cachingFlow() = runBlocking {
        val translation = TranslationEntity(
            readingUnitId = "u_101",
            bookId = "book_1",
            sourceText = "Computers process data.",
            odiaText = "କମ୍ପ୍ୟୁଟର ତଥ୍ୟ ପ୍ରକ୍ରିୟାକରଣ କରେ।",
            status = TranslationEntity.STATUS_TRANSLATED
        )
        db.translationDao().insertOrUpdateTranslation(translation)

        val cached = db.translationDao().getTranslation("u_101")
        assertNotNull(cached)
        assertEquals(TranslationEntity.STATUS_TRANSLATED, cached?.status)
        assertEquals("କମ୍ପ୍ୟୁଟର ତଥ୍ୟ ପ୍ରକ୍ରିୟାକରଣ କରେ।", cached?.odiaText)
    }

    @Test
    fun testBookmarkDao_addAndRetrieve() = runBlocking {
        val bookmark = BookmarkEntity(
            id = "bm_1",
            bookId = "book_1",
            pageNumber = 12,
            readingUnitId = "unit_7",
            readingUnitSnippet = "Sorting algorithms rearrange elements."
        )
        db.bookmarkDao().insertBookmark(bookmark)

        val retrieved = db.bookmarkDao().getBookmarkForUnit("book_1", "unit_7")
        assertNotNull(retrieved)
        assertEquals(12, retrieved?.pageNumber)
    }

    @Test
    fun testAudiobookDao_insertProgressAndSegments() = runBlocking {
        val audiobook = com.example.data.local.entity.AudiobookEntity(
            id = "ab_test_1",
            bookId = "book_1",
            title = "Introduction to Algorithms",
            languageMode = "BILINGUAL",
            status = com.example.data.local.entity.AudiobookEntity.STATUS_PROCESSING,
            progressPercent = 50,
            currentProcessingPage = 5,
            totalPages = 10,
            filePath = "/data/audiobooks/ab_test_1.mp3"
        )
        db.audiobookDao().insertOrUpdateAudiobook(audiobook)

        val retrieved = db.audiobookDao().getAudiobookById("ab_test_1")
        assertNotNull(retrieved)
        assertEquals("BILINGUAL", retrieved?.languageMode)
        assertEquals(50, retrieved?.progressPercent)

        // Insert segment
        val segment = com.example.data.local.entity.AudiobookSegmentEntity(
            id = "seg_1",
            audiobookId = "ab_test_1",
            segmentIndex = 0,
            pageNumber = 1,
            readingUnitId = "unit_1",
            textSpoken = "Algorithms are step-by-step procedures.",
            audioFilePath = "/data/audiobooks/seg_1.mp3",
            durationMs = 3500L,
            language = "en-US"
        )
        db.audiobookDao().insertSegments(listOf(segment))

        val segments = db.audiobookDao().getSegmentsForAudiobook("ab_test_1")
        assertEquals(1, segments.size)
        assertEquals("seg_1", segments[0].id)
    }

    @Test
    fun testReadingUnitDao_searchReadingUnits() = runBlocking {
        val u1 = ReadingUnitEntity(
            id = "b1_p1_u1",
            bookId = "book_search",
            pageNumber = 1,
            paragraphIndex = 1,
            unitIndexInPage = 1,
            englishText = "Binary search runs in logarithmic time."
        )
        val u2 = ReadingUnitEntity(
            id = "b1_p1_u2",
            bookId = "book_search",
            pageNumber = 1,
            paragraphIndex = 2,
            unitIndexInPage = 2,
            englishText = "Linear search runs in linear time."
        )
        db.readingUnitDao().insertReadingUnits(listOf(u1, u2))

        val results = db.readingUnitDao().searchReadingUnits("book_search", "logarithmic")
        assertEquals(1, results.size)
        assertEquals("b1_p1_u1", results[0].id)
    }
}
