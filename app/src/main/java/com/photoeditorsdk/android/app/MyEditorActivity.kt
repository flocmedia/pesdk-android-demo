package com.photoeditorsdk.android.app

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.chunk.MultiRect
import ly.img.android.pesdk.backend.model.config.ImageStickerAsset
import ly.img.android.pesdk.backend.model.state.EditorShowState
import ly.img.android.pesdk.backend.model.state.LayerListSettings
import ly.img.android.pesdk.backend.model.state.TransformSettings
import ly.img.android.pesdk.backend.model.state.layer.ImageStickerLayerSettings
import ly.img.android.pesdk.backend.model.state.layer.SpriteLayerSettings
import ly.img.android.pesdk.ui.activity.EditorActivity
import ly.img.android.pesdk.utils.MathUtils

class MyEditorActivity: EditorActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.addStickerButton).setOnClickListener {
            addSticker()
        }
    }

    private fun addSticker() {
        val spriteLayerSettings = stateHandler.createLayerSettingsModel(
            ImageStickerLayerSettings::class.java,
            ImageStickerAsset.createTemporaryStickerAsset(ImageSource.create(R.drawable.imgly_sticker_emoticons_star))
        ) as SpriteLayerSettings

        val editorShowState = stateHandler.getStateModel(EditorShowState::class.java)

        val imageHeight = editorShowState.imageRectF.height()
        val imageWidth = editorShowState.imageRectF.width()

        Log.d(TAG, "imageRectF: width=$imageWidth, height=$imageHeight")

        val visibleImageRegion = editorShowState.obtainVisibleImageRegion()

        Log.d(TAG, "visibleImageRegion: "
                + "left=${visibleImageRegion.left} "
                + "right=${visibleImageRegion.right} "
                + "top=${visibleImageRegion.top} "
                + "bottom=${visibleImageRegion.bottom} "
                + "width=${visibleImageRegion.width()} "
                + "height=${visibleImageRegion.height()}"
        )

        val realStageRect = editorShowState.realStageRect

        Log.d(TAG, "realStageRect: "
                + "left=${realStageRect.left} "
                + "right=${realStageRect.right} "
                + "top=${realStageRect.top} "
                + "bottom=${realStageRect.bottom} "
                + "width=${realStageRect.width()} "
                + "height=${realStageRect.height()}"
        )

        val visibleStage = Rect()
        editorShowState.getVisibleStage(visibleStage)

        Log.d(TAG, "visibleStage: "
                + "left=${visibleStage.left} "
                + "right=${visibleStage.right} "
                + "top=${visibleStage.top} "
                + "bottom=${visibleStage.bottom} "
                + "width=${visibleStage.width()} "
                + "height=${visibleStage.height()}"
        )

        val editor = findCustomEditorPreview(this.window.decorView.rootView as ViewGroup)!!

        val zoomOffsetX = editor.getZoomOffsetX()
        val zoomOffsetY = editor.getZoomOffsetY()
        val zoomScale = editor.getZoom()
        Log.d(TAG, "zoom: "
                + "zoomOffsetX=$zoomOffsetX "
                + "zoomOffsetY=$zoomOffsetY "
                + "zoomScale=$zoomScale"
        )

        val cropRect = MultiRect.obtain()
        stateHandler.getStateModel(TransformSettings::class.java).getCropRect(cropRect)

        Log.d(TAG, "cropRect: "
                + "left=${cropRect.left} "
                + "right=${cropRect.right} "
                + "top=${cropRect.top} "
                + "bottom=${cropRect.bottom} "
                + "width=${cropRect.width()} "
                + "height=${cropRect.height()}"
        )

        val x = MathUtils.mapRange(
            MathUtils.clamp(cropRect.width().toDouble()/2 + zoomOffsetX/zoomScale, cropRect.left.toDouble(), cropRect.right.toDouble()), 0.0, cropRect.width().toDouble(), 0.0, 1.0
        )

        val y = MathUtils.mapRange(
            MathUtils.clamp(cropRect.height().toDouble()/2 + zoomOffsetY/zoomScale, cropRect.top.toDouble(), cropRect.bottom.toDouble()), 0.0, cropRect.height().toDouble(), 0.0, 1.0
        )

        Log.d(TAG, "position: ($x,$y)")

        @Suppress("INACCESSIBLE_TYPE")
        spriteLayerSettings.setPosition(
            x,
            y,
            0f,
            0.5 / zoomScale
        )

        cropRect.recycle()

        val layerSettings = stateHandler.getSettingsModel(LayerListSettings::class.java)
        layerSettings.addAndSelectLayer(spriteLayerSettings)
    }

    private fun findCustomEditorPreview(container: ViewGroup): CustomEditorPreview? {
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is CustomEditorPreview) {
                return view
            }
            if (view is ViewGroup) {
                val editorPreview = findCustomEditorPreview(view)
                if (editorPreview != null) {
                    return editorPreview
                }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "MyEditorActivity"
    }
}