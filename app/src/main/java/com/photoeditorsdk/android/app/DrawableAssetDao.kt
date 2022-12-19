package com.photoeditorsdk.android.app

import androidx.room.*

@Dao
interface DrawableAssetDao {
    @Query("SELECT * FROM DrawableAsset WHERE remote = 1")
    suspend fun getAll(): List<DrawableAsset>
}