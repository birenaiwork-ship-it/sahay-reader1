package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ReadingUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingUnitDao {
    @Query("SELECT * FROM reading_units WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY unitIndexInPage ASC")
    fun getReadingUnitsForPageFlow(bookId: String, pageNumber: Int): Flow<List<ReadingUnitEntity>>

    @Query("SELECT * FROM reading_units WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY unitIndexInPage ASC")
    suspend fun getReadingUnitsForPage(bookId: String, pageNumber: Int): List<ReadingUnitEntity>

    @Query("SELECT * FROM reading_units WHERE bookId = :bookId ORDER BY pageNumber ASC, unitIndexInPage ASC")
    suspend fun getAllReadingUnitsForBook(bookId: String): List<ReadingUnitEntity>

    @Query("SELECT * FROM reading_units WHERE id = :unitId LIMIT 1")
    suspend fun getReadingUnitById(unitId: String): ReadingUnitEntity?

    @Query("SELECT * FROM reading_units WHERE bookId = :bookId AND englishText LIKE '%' || :query || '%' ORDER BY pageNumber ASC, unitIndexInPage ASC")
    suspend fun searchReadingUnits(bookId: String, query: String): List<ReadingUnitEntity>

    @Query("SELECT * FROM reading_units WHERE bookId = :bookId AND isHeading = 1 ORDER BY pageNumber ASC, unitIndexInPage ASC")
    fun getTableOfContents(bookId: String): Flow<List<ReadingUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingUnits(units: List<ReadingUnitEntity>)

    @Query("DELETE FROM reading_units WHERE bookId = :bookId")
    suspend fun deleteReadingUnitsForBook(bookId: String)
}
