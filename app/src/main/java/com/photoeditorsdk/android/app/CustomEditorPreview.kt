package com.photoeditorsdk.android.app

import android.content.Context
import android.util.AttributeSet
import ly.img.android.pesdk.backend.views.GlGround

class CustomEditorPreview
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GlGround(context, attrs) {

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val screenPos = IntArray(2)
        getLocationOnScreen(screenPos)
        showState.setPreviewSize(screenPos[0], screenPos[1], width, height)
        showState.callPreviewDirty()
    }

    fun getZoom() = GlGround::class.java.getDeclaredField("zoomScale").let { field ->
        field.isAccessible = true
        field.getFloat(this)
    }

    fun getZoomOffsetX() = GlGround::class.java.getDeclaredField("zoomOffsetX").let { field ->
        field.isAccessible = true
        field.getFloat(this)
    }

    fun getZoomOffsetY() = GlGround::class.java.getDeclaredField("zoomOffsetY").let { field ->
        field.isAccessible = true
        field.getFloat(this)
    }
}