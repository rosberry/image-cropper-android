package com.rosberry.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max

class ImageLoadHelper(private val context: Context) {

    fun getAssetOptions(fileName: String): BitmapFactory.Options {
        return getSourceOptions(context.resources.assets.open(fileName))
    }

    fun getFileOptions(uri: Uri): BitmapFactory.Options {
        return getSourceOptions(context.contentResolver.openInputStream(uri))
    }

    fun getResourceOptions(resId: Int): BitmapFactory.Options {
        val options = BitmapFactory.Options()
            .apply { inJustDecodeBounds = true }

        BitmapFactory.decodeResource(context.resources, resId, options)
        return options
    }

    fun getPreviewBitmap(
            uri: Uri,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap? {
        return getPreviewBitmap(context.contentResolver.openInputStream(uri), width, height, options)
    }

    fun getPreviewBitmap(
            assetName: String,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap? {
        return getPreviewBitmap(context.resources.assets.open(assetName), width, height, options)
    }

    fun getPreviewBitmap(
            resId: Int,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId, getPreviewOptions(width, height, options))
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

    private fun getSourceOptions(inputStream: InputStream?): BitmapFactory.Options {
        val options = BitmapFactory.Options()
            .apply { inJustDecodeBounds = true }

        inputStream.use { BitmapFactory.decodeStream(it, null, options) }
        return options
    }

    private fun getPreviewOptions(width: Int, height: Int, options: BitmapFactory.Options): BitmapFactory.Options {
        val maxSize = max(width, height) * 2
        val maxImageDimen = max(options.outWidth, options.outHeight)
        val bitmapSize = 4 * options.outWidth * options.outHeight
        val runtime = Runtime.getRuntime()
        val availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        var inSampleSize = 1

        while (maxImageDimen / inSampleSize > maxSize || bitmapSize / (2 * inSampleSize) > availableMemory) {
            inSampleSize *= 2
        }

        return BitmapFactory.Options()
            .apply { this.inSampleSize = inSampleSize }
    }

    private fun getRotation(exif: ExifInterface): Float {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
}