package com.example.data.repository

import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao
) {

    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForBook(bookId)
    }

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getAllBookmarks()
    }

    suspend fun isBookmarked(bookId: String, unitId: String): Boolean {
        return bookmarkDao.getBookmarkForUnit(bookId, unitId) != null
    }

    suspend fun toggleBookmark(
        bookId: String,
        pageNumber: Int,
        readingUnitId: String,
        readingUnitSnippet: String,
        note: String? = null
    ): Boolean {
        val existing = bookmarkDao.getBookmarkForUnit(bookId, readingUnitId)
        return if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false // removed
        } else {
            val bookmark = BookmarkEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                pageNumber = pageNumber,
                readingUnitId = readingUnitId,
                readingUnitSnippet = readingUnitSnippet.take(120),
                note = note,
                createdAt = System.currentTimeMillis()
            )
            bookmarkDao.insertBookmark(bookmark)
            true // added
        }
    }

    suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun deleteBookmarksForBook(bookId: String) {
        bookmarkDao.deleteBookmarksForBook(bookId)
    }
}
