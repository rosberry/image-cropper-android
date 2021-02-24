package com.rosberry.android.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import java.io.InputStream
import kotlin.math.max

internal class AssetSource(context: Context, private val fileName: String) : ImageSource<String>(context) {

    override val inputStream get() = context.resources.assets.open(fileName)
}

internal class MediaSource(context: Context, private val uri: Uri) : ImageSource<Uri>(context) {

    private val rotation: Float by lazy {
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.ORIENTATION), null, null, null)
            ?.use {
                return@use if (it.count != 1) {
                    0f
                } else {
                    it.moveToFirst()
                    it.getInt(0)
                        .toFloat()
                }
            } ?: 0f
    }

    override val inputStream get() = context.contentResolver.openInputStream(uri)

    override fun getPreviewBitmap(width: Int, height: Int): Bitmap? {
        inputStream.use {
            val source = BitmapFactory.decodeStream(it, null, getPreviewOptions(width, height, options))

            if (source != null && rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

                source.recycle()
                return bitmap
            }
            return source
        }
    }
}

internal class ResourceSource(context: Context, private val resId: Int) : ImageSource<Int>(context) {

    override val inputStream get() = context.resources.openRawResource(resId)
}

sealed class ImageSource<T : Any>(protected val context: Context) {

    val options by lazy { getDecodeOptions() }

    protected abstract val inputStream: InputStream?

    open fun getPreviewBitmap(width: Int, height: Int): Bitmap? {
        return inputStream.use { BitmapFactory.decodeStream(it, null, getPreviewOptions(width, height, options)) }
    }

    fun getCroppedBitmap(cropRect: Rect): Bitmap? {
        inputStream.use {
            val options = getCropOptions(cropRect)
            val decoder = BitmapRegionDecoder.newInstance(it, false)
            val bitmap = try {
                decoder.decodeRegion(cropRect, options)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }

            decoder.recycle()
            return bitmap
        }
    }

    protected fun getPreviewOptions(width: Int, height: Int, options: BitmapFactory.Options): BitmapFactory.Options {
        val maxSize = max(width, height) * 2
        val maxImageDimen = max(options.outWidth, options.outHeight)
        val bitmapSize = options.getBitmapSize()
        val availableMemory = getAvailableMemory()
        val previewOptions = BitmapFactory.Options()
        previewOptions.inSampleSize = if (bitmapSize > availableMemory) 2 else 1

        while (maxImageDimen / previewOptions.inSampleSize > maxSize || bitmapSize / (2 * previewOptions.inSampleSize) > availableMemory) {
            previewOptions.inSampleSize *= 2
        }

        return previewOptions
    }

    private fun getCropOptions(cropRect: Rect): BitmapFactory.Options {
        val bitmapSize = 4 * cropRect.width() * cropRect.height()
        val availableMemory = getAvailableMemory()
        val options = BitmapFactory.Options()
        options.inSampleSize = if (bitmapSize > availableMemory) 2 else 1

        while (bitmapSize / (2 * options.inSampleSize) > availableMemory) {
            options.inSampleSize *= 2
        }

        return options
    }

    private fun getDecodeOptions(): BitmapFactory.Options {
        return BitmapFactory.Options()
            .apply {
                inJustDecodeBounds = true
                inputStream.use { BitmapFactory.decodeStream(it, null, this) }
            }
    }

    private fun getAvailableMemory(): Long = with(Runtime.getRuntime()) { maxMemory() - (totalMemory() - freeMemory()) }

    private fun BitmapFactory.Options.getBitmapSize(): Int = 4 * outWidth * outHeight
}