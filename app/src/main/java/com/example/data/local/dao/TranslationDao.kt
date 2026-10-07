package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations WHERE readingUnitId = :unitId LIMIT 1")
    fun getTranslationFlow(unitId: String): Flow<TranslationEntity?>

    @Query("SELECT * FROM translations WHERE readingUnitId = :unitId LIMIT 1")
    suspend fun getTranslation(unitId: String): TranslationEntity?

    @Query("SELECT * FROM translations WHERE bookId = :bookId")
    suspend fun getAllTranslationsForBook(bookId: String): List<TranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTranslation(translation: TranslationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<TranslationEntity>)

    @Query("DELETE FROM translations WHERE bookId = :bookId")
    suspend fun deleteTranslationsForBook(bookId: String)

    @Query("DELETE FROM translations")
    suspend fun clearAllTranslations()
}
