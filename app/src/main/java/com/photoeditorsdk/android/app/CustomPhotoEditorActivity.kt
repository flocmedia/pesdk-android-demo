package com.photoeditorsdk.android.app

import android.widget.Toast
import ly.img.android.pesdk.ui.activity.PhotoEditorActivity
import ly.img.android.serializer._3.IMGLYFileWriter
import java.io.File
import java.io.IOException

class CustomPhotoEditorActivity: PhotoEditorActivity() {
    private val file by lazy {
        File(filesDir, "serialisationReadyToReadWithPESDKFileReader.json")
    }

    override fun onStop() {
        val lastState = stateHandler.createSettingsListDump()
        try {
            IMGLYFileWriter(lastState).writeJson(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        lastState.release()

        super.onStop()
    }
}