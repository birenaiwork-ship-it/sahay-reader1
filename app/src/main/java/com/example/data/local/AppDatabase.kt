package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AudiobookDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.PageDao
import com.example.data.local.dao.ReadingUnitDao
import com.example.data.local.dao.TranslationDao
import com.example.data.local.entity.AudiobookEntity
import com.example.data.local.entity.AudiobookSegmentEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ReadingUnitEntity
import com.example.data.local.entity.TranslationEntity

@Database(
    entities = [
        BookEntity::class,
        PageEntity::class,
        ReadingUnitEntity::class,
        TranslationEntity::class,
        BookmarkEntity::class,
        AudiobookEntity::class,
        AudiobookSegmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun pageDao(): PageDao
    abstract fun readingUnitDao(): ReadingUnitDao
    abstract fun translationDao(): TranslationDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun audiobookDao(): AudiobookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sahaya_reader.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
