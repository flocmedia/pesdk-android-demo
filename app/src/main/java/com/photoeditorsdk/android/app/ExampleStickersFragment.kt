package com.photoeditorsdk.android.app

import android.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.ui.sticker.custom.CustomStickersFragment
import java.io.ByteArrayOutputStream


class ExampleStickersFragment : CustomStickersFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return Button(requireContext()).apply {
            text = "Add"

            setOnClickListener {
                ContextCompat.getDrawable(
                    context,
                    ly.img.android.pesdk.assets.sticker.emoticons.R.drawable.imgly_sticker_emoticons_hitman
                )?.let {
                    drawableToBitmap(it)?.let {
                        stickerSelectedListener.onStickerSelected(
                            ImageSource.createFromBase64String(encodeToBase64(it))
                        )
                    }
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap =
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun encodeToBase64(
        image: Bitmap,
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): String {
        val byteArrayOS = ByteArrayOutputStream()
        image.compress(compressFormat, quality, byteArrayOS)
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT)
    }
}