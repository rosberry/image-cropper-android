package com.rosberry.imagecropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ImageLoadHelper(private val context: Context) {

    fun loadBitmap(
            uri: Uri,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap? {
        return loadBitmap(context.contentResolver.openInputStream(uri), width, height, options)
    }

    fun loadBitmap(
            assetName: String,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap? {
        return loadBitmap(context.resources.assets.open(assetName), width, height, options)
    }

    fun loadBitmap(
            resId: Int,
            width: Int,
            height: Int,
            options: BitmapFactory.Options
    ): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId, getDecodeOptions(width, height, options))
    }

    private fun loadBitmap(inputStream: InputStream?, width: Int, height: Int, options: BitmapFactory.Options): Bitmap? {
        inputStream.use {
            val source = BitmapFactory.decodeStream(it, null, getDecodeOptions(width, height, options)) ?: return null

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

    private fun getDecodeOptions(width: Int, height: Int, options: BitmapFactory.Options): BitmapFactory.Options {
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