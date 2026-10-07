package com.example.data.repository

import com.example.data.local.dao.BookDao
import com.example.data.local.dao.PageDao
import com.example.data.local.dao.ReadingUnitDao
import com.example.data.local.dao.TranslationDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.AudiobookDao
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ReadingUnitEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookRepository(
    private val bookDao: BookDao,
    private val pageDao: PageDao,
    private val readingUnitDao: ReadingUnitDao,
    private val translationDao: TranslationDao,
    private val bookmarkDao: BookmarkDao,
    private val audiobookDao: AudiobookDao
) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookByIdFlow(bookId: String): Flow<BookEntity?> = bookDao.getBookByIdFlow(bookId)

    suspend fun getBookById(bookId: String): BookEntity? = bookDao.getBookById(bookId)

    fun getMostRecentBookFlow(): Flow<BookEntity?> = bookDao.getMostRecentBookFlow()

    fun getPagesForBook(bookId: String): Flow<List<PageEntity>> = pageDao.getPagesForBook(bookId)

    fun getReadingUnitsForPageFlow(bookId: String, pageNumber: Int): Flow<List<ReadingUnitEntity>> =
        readingUnitDao.getReadingUnitsForPageFlow(bookId, pageNumber)

    suspend fun getReadingUnitsForPage(bookId: String, pageNumber: Int): List<ReadingUnitEntity> =
        readingUnitDao.getReadingUnitsForPage(bookId, pageNumber)

    suspend fun getAllReadingUnitsForBook(bookId: String): List<ReadingUnitEntity> =
        readingUnitDao.getAllReadingUnitsForBook(bookId)

    suspend fun getReadingUnitById(unitId: String): ReadingUnitEntity? =
        readingUnitDao.getReadingUnitById(unitId)

    fun getTableOfContents(bookId: String): Flow<List<ReadingUnitEntity>> =
        readingUnitDao.getTableOfContents(bookId)

    suspend fun searchReadingUnits(bookId: String, query: String): List<ReadingUnitEntity> =
        readingUnitDao.searchReadingUnits(bookId, query)

    suspend fun saveBook(
        book: BookEntity,
        pages: List<PageEntity>,
        readingUnits: List<ReadingUnitEntity>
    ) {
        bookDao.insertOrUpdateBook(book)
        pageDao.insertPages(pages)
        readingUnitDao.insertReadingUnits(readingUnits)
    }

    suspend fun updateReadingPosition(bookId: String, page: Int, unitId: String?) {
        bookDao.updateReadingPosition(bookId, page, unitId)
    }

    suspend fun deleteBookCompletely(bookId: String, filesDir: File) {
        // Delete saved PDF file
        try {
            File(filesDir, "books/${bookId}.pdf").delete()
        } catch (_: Exception) {}

        // Delete audio files associated with book audiobooks
        val audiobooks = audiobookDao.getLatestAudiobookForBook(bookId)
        audiobooks?.filePath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }

        // Delete from DB
        bookmarkDao.deleteBookmarksForBook(bookId)
        translationDao.deleteTranslationsForBook(bookId)
        readingUnitDao.deleteReadingUnitsForBook(bookId)
        pageDao.deletePagesForBook(bookId)
        audiobookDao.getLatestAudiobookForBook(bookId)?.let {
            audiobookDao.deleteAudiobookById(it.id)
        }
        bookDao.deleteBookById(bookId)
    }
}
