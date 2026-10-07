package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AudiobookEntity
import com.example.data.local.entity.AudiobookSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @Query("SELECT * FROM audiobooks ORDER BY createdAt DESC")
    fun getAllAudiobooksFlow(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :audiobookId LIMIT 1")
    fun getAudiobookByIdFlow(audiobookId: String): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE id = :audiobookId LIMIT 1")
    suspend fun getAudiobookById(audiobookId: String): AudiobookEntity?

    @Query("SELECT * FROM audiobooks WHERE bookId = :bookId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAudiobookForBook(bookId: String): AudiobookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAudiobook(audiobook: AudiobookEntity)

    @Update
    suspend fun updateAudiobook(audiobook: AudiobookEntity)

    @Query("UPDATE audiobooks SET lastPlaybackPositionMs = :positionMs WHERE id = :audiobookId")
    suspend fun updatePlaybackPosition(audiobookId: String, positionMs: Long)

    @Query("UPDATE audiobooks SET progressPercent = :progress, currentProcessingPage = :page, currentReadingUnitIndex = :unitIndex, status = :status WHERE id = :audiobookId")
    suspend fun updateProgress(audiobookId: String, progress: Int, page: Int, unitIndex: Int, status: String)

    @Query("DELETE FROM audiobooks WHERE id = :audiobookId")
    suspend fun deleteAudiobookById(audiobookId: String)

    // Segments
    @Query("SELECT * FROM audiobook_segments WHERE audiobookId = :audiobookId ORDER BY segmentIndex ASC")
    suspend fun getSegmentsForAudiobook(audiobookId: String): List<AudiobookSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<AudiobookSegmentEntity>)

    @Query("DELETE FROM audiobook_segments WHERE audiobookId = :audiobookId")
    suspend fun deleteSegmentsForAudiobook(audiobookId: String)
}
