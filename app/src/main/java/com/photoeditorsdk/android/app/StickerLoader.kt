package com.photoeditorsdk.android.app

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object StickerLoader {
    const val TARGET_SIZE = 350

    private const val STICKER_REMOTE_DIRECTORY = "https://storage.googleapis.com/thug-life-asset-bucket/stickers/"
    private const val STICKER_REMOTE_DIRECTORY_FORMAT_STRING = "$STICKER_REMOTE_DIRECTORY%s.webp"

    private fun createStickerUrl(resourceName: String) = String.format(
        STICKER_REMOTE_DIRECTORY_FORMAT_STRING,
        resourceName
    )

    private suspend fun <ResourceType> loadStickerUrl(
        context: Context,
        resourceName: String,
        clazz: Class<ResourceType>,
        cacheOpts: CacheOpts
    ): ResourceType? = withContext(Dispatchers.IO) {
        val stickerUrl = createStickerUrl(resourceName)

        return@withContext try {
            var request = Glide.with(context).`as`(clazz)
                .load(stickerUrl)
                .timeout(15000)
                .apply(
                    RequestOptions().override(
                        TARGET_SIZE,
                        TARGET_SIZE
                    )
                )
                .dontAnimate()

            if (cacheOpts == CacheOpts.SKIP_CACHE) {
                request = request.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
            } else if (cacheOpts == CacheOpts.RETRIEVE_FROM_CACHE) {
                request = request.onlyRetrieveFromCache(true)
            }

            request.submit().get()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadHiResSticker(
        context: Context,
        resourceName: String,
        cacheOpts: CacheOpts = CacheOpts.NONE
    ): File? {
        return loadHiResStickerRoutine(context, resourceName, File::class.java, cacheOpts)
    }

    private suspend fun <ResourceType> loadHiResStickerRoutine(
        context: Context,
        resourceName: String,
        clazz: Class<ResourceType>,
        cacheOpts: CacheOpts = CacheOpts.NONE
    ): ResourceType? {
        val highResDrawable = retry(times = 1, initialDelay = 500L, mutateDelay = false) {
            loadStickerUrl(context, resourceName, clazz, cacheOpts)
        }

        return highResDrawable
    }

    private suspend fun <T> retry(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 1000,    // 1 second
        factor: Double = 2.0,
        mutateDelay: Boolean = true, //so initial delay will be affected by factor or not
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(times) {
            val result = block()
            if (result != null) {
                return result
            } else {
                delay(currentDelay)
                if (mutateDelay) {
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                }
            }
        }
        return block() // last attempt
    }

    enum class CacheOpts {
        SKIP_CACHE,
        RETRIEVE_FROM_CACHE,
        NONE
    }
}