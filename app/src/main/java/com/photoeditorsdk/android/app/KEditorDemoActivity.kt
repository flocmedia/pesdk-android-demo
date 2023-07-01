package com.photoeditorsdk.android.app

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.android.pesdk.PhotoEditorSettingsList
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic
import ly.img.android.pesdk.assets.sticker.emoticons.StickerPackEmoticons
import ly.img.android.pesdk.assets.sticker.shapes.StickerPackShapes
import ly.img.android.pesdk.backend.model.EditorSDKResult
import ly.img.android.pesdk.backend.model.constant.ImageExportFormat
import ly.img.android.pesdk.backend.model.constant.OutputMode
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.PhotoEditorSaveSettings
import ly.img.android.pesdk.backend.operator.headless.DocumentRenderWorker
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.pesdk.ui.model.state.UiConfigFilter
import ly.img.android.pesdk.ui.model.state.UiConfigFrame
import ly.img.android.pesdk.ui.model.state.UiConfigOverlay
import ly.img.android.pesdk.ui.model.state.UiConfigSticker
import ly.img.android.pesdk.ui.model.state.UiConfigText
import ly.img.android.pesdk.ui.panels.item.PersonalStickerAddItem
import java.io.File

class KEditorDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KEditorDemoActivity"
        const val PESDK_RESULT = 1
        const val GALLERY_RESULT = 2
    }

    // Create a empty new SettingsList and apply the changes on this reference.
    // If you include our asset Packs and use our UI you also need to add them to the UI Config,
    // otherwise they are only available for the backend (like Serialisation)
    // See the specific feature sections of our guides if you want to know how to add your own Assets.
    private fun createPesdkSettingsList() =
      PhotoEditorSettingsList(true)
        .configure<UiConfigFilter> {
            it.setFilterList(FilterPackBasic.getFilterPack())
        }
        .configure<UiConfigText> {
            it.setFontList(FontPackBasic.getFontPack())
        }
        .configure<UiConfigFrame> {
            it.setFrameList(FramePackBasic.getFramePack())
        }
        .configure<UiConfigOverlay> {
            it.setOverlayList(OverlayPackBasic.getOverlayPack())
        }
        .configure<UiConfigSticker> {
            it.setStickerLists(
                PersonalStickerAddItem(),
                StickerPackEmoticons.getStickerCategory(),
                StickerPackShapes.getStickerCategory()
            )
        }
        .configure<PhotoEditorSaveSettings> {
            // Set custom editor image export settings
            it.setOutputToUri(Uri.fromFile(getOutputFile()))
            it.outputMode = OutputMode.EXPORT_ONLY_SETTINGS_LIST
            it.setExportFormat(ImageExportFormat.PNG)
        }

    private fun getOutputFile() = File(filesDir, "imgly_photo.jpg")

    private fun Resources.getRawUri(@RawRes rawRes: Int) = "%s://%s/%s/%s".format(
        ContentResolver.SCHEME_ANDROID_RESOURCE, this.getResourcePackageName(rawRes),
        this.getResourceTypeName(rawRes), this.getResourceEntryName(rawRes)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openGallery = findViewById<Button>(R.id.openGallery)

        openGallery.setOnClickListener {
            openEditor(Uri.parse(resources.getRawUri(R.raw.big_image)))
//            openSystemGalleryToSelectAnImage()
        }
    }

    private suspend fun waitForClosingFile(file: File) = withContext(Dispatchers.IO) {
        var isFileModified = false

        val fileObserver = if (Build.VERSION.SDK_INT >= 29) {
            object : FileObserver(file) {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "onEvent: ${Integer.toHexString(event)}, path: $path")

                    isFileModified = true
                }
            }
        }
        else {
            object : FileObserver(file.absolutePath) {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "onEvent: ${Integer.toHexString(event)}, path: $path")

                    isFileModified = true
                }
            }
        }

        fileObserver.startWatching()

        while (true) {
            delay(1000)

            if (!isFileModified) break

            isFileModified = false
        }

        fileObserver.stopWatching()
    }

    fun openSystemGalleryToSelectAnImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(intent, GALLERY_RESULT)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No Gallery APP installed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openEditor(inputImage: Uri?) {
        val settingsList = createPesdkSettingsList()

        settingsList.configure<LoadSettings> {
            it.source = inputImage
        }

        PhotoEditorBuilder(this)
          .setSettingsList(settingsList)
          .startActivityForResult(this, PESDK_RESULT)

        settingsList.release()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        intent ?: return

        if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            // Open Editor with some uri in this case with an image selected from the system gallery.
            val selectedImage = intent.data
            if (selectedImage != null) {
                openEditor(selectedImage)
            }
        } else if (resultCode == RESULT_OK && requestCode == PESDK_RESULT) {
            // Editor has saved an Image.
            val result = EditorSDKResult(intent)

            when (result.resultStatus) {
                EditorSDKResult.Status.DONE_WITHOUT_EXPORT -> {
                    // Export the photo in background using WorkManager
                    result.settingsList.use { document ->
                        val workRequest = DocumentRenderWorker.createWorker(document)
                        WorkManager.getInstance(this).enqueue(workRequest)
                        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
                            .observe(this) { job ->
                                Log.d(
                                    TAG,
                                    "State: ${job.state} Progress: ${
                                        job.progress.getFloat(
                                            DocumentRenderWorker.FLOAT_PROGRESS_KEY,
                                            1f
                                        )
                                    }"
                                )

                                if (job.state == WorkInfo.State.SUCCEEDED) {
                                    lifecycleScope.launch {
                                        waitForClosingFile(getOutputFile())
                                    }
                                }
                            }
                    }
                }
                else -> {
                }
            }

        } else if (resultCode == RESULT_CANCELED && requestCode == PESDK_RESULT) {
            // Editor was canceled
            val data = EditorSDKResult(intent)

            val sourceURI = data.sourceUri
            // TODO: Do something with the source...*/
        }
    }

}
