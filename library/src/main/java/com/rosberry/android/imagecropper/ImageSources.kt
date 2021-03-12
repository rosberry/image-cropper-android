package com.rosberry.android.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max

internal class AssetSource(context: Context, private val fileName: String) : ImageSource<String>(context) {

    override val inputStream get() = context.resources.assets.open(fileName)
}

internal class MediaSource(context: Context, private val uri: Uri) : ImageSource<Uri>(context) {

    override val inputStream get() = context.contentResolver.openInputStream(uri)
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

    protected open val orientation by lazy {
        inputStream?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) }
    }

    protected open val matrix by lazy {
        Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
            when (orientation) {
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
    }

    fun getPreviewBitmap(width: Int, height: Int): Bitmap? {
        var bitmap = inputStream?.use { BitmapFactory.decodeStream(it, null, getPreviewOptions(width, height, options)) } ?: return null

        if (orientation != ExifInterface.ORIENTATION_NORMAL || orientation != ExifInterface.ORIENTATION_UNDEFINED) {
            bitmap = bitmap.applyMatrix(matrix)
        }
        return bitmap
    }

    fun getCroppedBitmap(previewWidth: Float, frameWidth: Float, frameHeight: Float, translation: PointF): Bitmap? {
        val cropRect = getCropRect(previewWidth, frameWidth, frameHeight, translation)
        val options = getCropOptions(cropRect)

        var bitmap = inputStream.use {
            val decoder = BitmapRegionDecoder.newInstance(it, false)

            try {
                return@use decoder.decodeRegion(cropRect, options)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return@use null
            } finally {
                decoder.recycle()
            }
        } ?: return null

        if (orientation != ExifInterface.ORIENTATION_NORMAL || orientation != ExifInterface.ORIENTATION_UNDEFINED) {
            bitmap = bitmap.applyMatrix(matrix)
        }
        return bitmap
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

    private fun getCropRect(previewWidth: Float, frameWidth: Float, frameHeight: Float, translation: PointF): Rect {
        val imageWidth = when (orientation) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> options.outHeight
            else -> options.outWidth
        }
        val imageHeight = when (orientation) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> options.outWidth
            else -> options.outHeight
        }
        val scale = imageWidth / previewWidth
        val cropWidth = (frameWidth * scale).toInt()
        val cropHeight = (frameHeight * scale).toInt()
        val x0 = (imageWidth - cropWidth) / 2f
        val y0 = (imageHeight - cropHeight) / 2f
        val left: Int = when (orientation) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90 -> y0 - translation.y * scale
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_ROTATE_180 -> x0 + translation.x * scale
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270 -> y0 + translation.y * scale
            else -> x0 - translation.x * scale
        }.toInt()
        val top: Int = when (orientation) {
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_90 -> x0 + translation.x * scale
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_ROTATE_180 -> y0 + translation.y * scale
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_270 -> x0 - translation.x * scale
            else -> y0 - translation.y * scale
        }.toInt()

        return when (orientation) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> Rect(left, top, left + cropHeight, top + cropWidth)
            else -> Rect(left, top, left + cropWidth, top + cropHeight)
        }
    }

    private fun getAvailableMemory(): Long = with(Runtime.getRuntime()) { maxMemory() - (totalMemory() - freeMemory()) }

    private fun Bitmap.applyMatrix(matrix: Matrix): Bitmap {
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }

    private fun BitmapFactory.Options.getBitmapSize(): Int = 4 * outWidth * outHeight
}