package com.rosberry.android.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max

internal class AssetSource(context: Context, private val fileName: String) : ImageSource<String>(context) {

    override val inputStream get() = context.resources.assets.open(fileName)
}

internal class MediaSource(context: Context, private val uri: Uri) : ImageSource<Uri>(context) {

    override val inputStream get() = context.contentResolver.openInputStream(uri)

    override fun getRotation(inputStream: InputStream): Float {
        return context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.ORIENTATION), null, null, null)
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
}

internal class ResourceSource(context: Context, private val resId: Int) : ImageSource<Int>(context) {

    override val inputStream get() = context.resources.openRawResource(resId)
}

sealed class ImageSource<T : Any>(protected val context: Context) {
    val options by lazy {
        BitmapFactory.Options()
            .apply {
                inJustDecodeBounds = true
                inputStream.use { BitmapFactory.decodeStream(it, null, this) }
            }
    }

    protected abstract val inputStream: InputStream?

    protected open var rotation: Float = 0f

    fun getPreviewBitmap(width: Int, height: Int): Bitmap? {
        return inputStream.use {
            var bitmap = BitmapFactory.decodeStream(it, null, getPreviewOptions(width, height, options)) ?: return@use null

            rotation = it?.let { getRotation(it) } ?: 0f

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            return@use bitmap
        }
    }

    fun getCroppedBitmap(previewWidth: Int, frameWidth: Float, frameHeight: Float, translation: PointF, imageScale: Float): Bitmap? {
        val cropRect = getCropRect(previewWidth, frameWidth, frameHeight, translation, imageScale)

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

    protected open fun getRotation(inputStream: InputStream): Float {
        return when (ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun getPreviewOptions(width: Int, height: Int, options: BitmapFactory.Options): BitmapFactory.Options {
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

    private fun getCropRect(previewWidth: Int, frameWidth: Float, frameHeight: Float, translation: PointF, imageScale: Float): Rect {
        val imageWidth = if (rotation == 90f || rotation == 270f) options.outHeight else options.outWidth
        val previewScale = imageWidth / previewWidth.toFloat()
        val scale = previewScale / imageScale
        val width = (frameWidth * scale).toInt()
        val height = (frameHeight * scale).toInt()
        val x0 = (options.outWidth - width) / 2f
        val y0 = (options.outHeight - height) / 2f
        val left = (x0 - translation.x * scale).toInt()
        val top = (y0 - translation.y * scale).toInt()

        return Rect(left, top, left + width, top + height)
    }

    private fun getAvailableMemory(): Long = with(Runtime.getRuntime()) { maxMemory() - (totalMemory() - freeMemory()) }

    private fun BitmapFactory.Options.getBitmapSize(): Int = 4 * outWidth * outHeight
}