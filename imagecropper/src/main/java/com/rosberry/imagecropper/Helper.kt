package com.rosberry.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max

class Helper(private val context: Context) {

    fun getDecodeOptions(fileName: String): BitmapFactory.Options = getDecodeOptions(getAssetStream(fileName))

    fun getDecodeOptions(uri: Uri): BitmapFactory.Options = getDecodeOptions(getFileStream(uri))

    fun getDecodeOptions(resId: Int): BitmapFactory.Options = getDecodeOptions(getResourceStream(resId))

    fun getCroppedBitmap(fileName: String, cropRect: Rect): Bitmap? = getCroppedBitmap(getAssetStream(fileName), cropRect)

    fun getCroppedBitmap(uri: Uri, cropRect: Rect): Bitmap? = getCroppedBitmap(getFileStream(uri), cropRect)

    fun getCroppedBitmap(resId: Int, cropRect: Rect): Bitmap? = getCroppedBitmap(getResourceStream(resId), cropRect)

    fun getPreviewBitmap(uri: Uri, width: Int, height: Int, options: BitmapFactory.Options): Bitmap? =
            getPreviewBitmap(getFileStream(uri), width, height, options)

    fun getPreviewBitmap(fileName: String, width: Int, height: Int, options: BitmapFactory.Options): Bitmap? =
            getPreviewBitmap(getAssetStream(fileName), width, height, options)

    fun getPreviewBitmap(resId: Int, width: Int, height: Int, options: BitmapFactory.Options): Bitmap? =
            getPreviewBitmap(getResourceStream(resId), width, height, options)

    private fun getDecodeOptions(inputStream: InputStream?): BitmapFactory.Options {
        return createDecodeOptions().apply {
            inputStream.use { BitmapFactory.decodeStream(it, null, this) }
        }
    }

    private fun createDecodeOptions(): BitmapFactory.Options {
        return BitmapFactory.Options()
            .apply { inJustDecodeBounds = true }
    }

    private fun getFileStream(uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)

    private fun getAssetStream(fileName: String): InputStream = context.resources.assets.open(fileName)

    private fun getResourceStream(resId: Int): InputStream = context.resources.openRawResource(resId)

    private fun getCroppedBitmap(inputStream: InputStream?, cropRect: Rect): Bitmap? {
        inputStream.use {
            val bitmapSize = 4 * cropRect.width() * cropRect.height()
            val availableMemory = getAvailableMemory()
            val options = BitmapFactory.Options()
            options.inSampleSize = if (bitmapSize > availableMemory) 2 else 1

            while (bitmapSize / (2 * options.inSampleSize) > availableMemory) {
                options.inSampleSize *= 2
            }

            val decoder = BitmapRegionDecoder.newInstance(it, false)
            val bitmap = decoder.decodeRegion(cropRect, options)

            decoder.recycle()
            return bitmap
        }
    }

    private fun getPreviewBitmap(inputStream: InputStream?, width: Int, height: Int, options: BitmapFactory.Options): Bitmap? {
        inputStream.use {
            val source = BitmapFactory.decodeStream(it, null, getPreviewOptions(width, height, options)) ?: return null

            val rotation = it?.let { getRotation(ExifInterface(it)) } ?: 0f
            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

                source.recycle()
                return bitmap
            }

            return source
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

    private fun getAvailableMemory(): Long = with(Runtime.getRuntime()) { maxMemory() - (totalMemory() - freeMemory()) }

    private fun getRotation(exif: ExifInterface): Float {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun BitmapFactory.Options.getBitmapSize(): Int = 4 * outWidth * outHeight
}