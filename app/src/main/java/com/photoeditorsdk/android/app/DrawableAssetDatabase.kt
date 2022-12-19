package com.photoeditorsdk.android.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.photoeditorsdk.android.app.DrawableAssetDatabase.Companion.DATABASE_VERSION

@Database(entities = [DrawableAsset::class], version = DATABASE_VERSION, exportSchema = false)
abstract class DrawableAssetDatabase : RoomDatabase() {
    abstract fun DrawableAssetDao(): DrawableAssetDao

    companion object {
        const val DATABASE_VERSION = 12

        private const val DATABASE_NAME = "thug-life-database"

        @JvmStatic private fun buildDatabase(
            context: Context,
        ): DrawableAssetDatabase {
            return Room.databaseBuilder(
                context,
                DrawableAssetDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .createFromAsset("database/thug-life-database-initial.db")
                .build()
        }

        private var instance: DrawableAssetDatabase? = null

        fun getInstance(context: Context): DrawableAssetDatabase {
            if (instance == null) {
                synchronized(DrawableAssetDatabase::class.java) {
                    if (instance == null) {
                        instance = buildDatabase(context)
                    }
                }
            }

            return instance as DrawableAssetDatabase
        }
    }
}