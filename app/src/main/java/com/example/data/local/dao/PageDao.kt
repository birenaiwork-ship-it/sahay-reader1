package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getPagesForBook(bookId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getPage(bookId: String, pageNumber: Int): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Query("DELETE FROM pages WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: String)
}
