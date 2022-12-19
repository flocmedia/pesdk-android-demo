package com.photoeditorsdk.android.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class DrawableAsset(
    val stickerId: Int,
    @ColumnInfo(name = "drawableAssetId") private val _drawableAssetId: String?,
    @ColumnInfo(name = "category") private val _category: String?,
    var locked: Int,
    val remote: Int
) {
    @PrimaryKey(autoGenerate = true) var id = 0

    val drawableAssetId: String get() = _drawableAssetId!!
    val category: String get() = _category!!
}