package com.rosberry.imagecropper

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class CropHelper(private val context: Context) {

    fun getBitmap(uri: Uri): Bitmap {
        val source = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            true -> decodeBitmap(uri)
            false -> getBitmapCompat(uri)
        }
        val maxSize = context.resources.displayMetrics.heightPixels
        val matrix = Matrix()

        if (max(source.width, source.height) > maxSize) {
            val ratio = source.width / source.height.toFloat()
            val height: Float
            val width: Float

            if (ratio < 0) {
                height = min(maxSize, source.height).toFloat()
                width = (height * ratio)
            } else {
                width = min(maxSize, source.width).toFloat()
                height = (width / ratio)
            }
            matrix.setScale(width / source.width, height / source.height)
        }
        matrix.postRotate(getRotation(uri))

        val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        if (bitmap != source) {
            source.recycle()
        }
        return bitmap
    }

    fun getCroppedImage(
            bitmap: Bitmap,
            translation: PointF,
            frameWidth: Float,
            frameHeight: Float,
            relativeScale: Float
    ): Bitmap? {
        return try {
            val width = (frameWidth * relativeScale).toInt()
            val height = (frameHeight * relativeScale).toInt()
            val startX = (bitmap.width - width) / 2f
            val startY = (bitmap.height - height) / 2f
            val x = (startX - translation.x * relativeScale).toInt()
            val y = (startY - translation.y * relativeScale).toInt()

            Bitmap.createBitmap(bitmap, x, y, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRotation(uri: Uri): Float {
        return try {
            val exif = context.contentResolver
                .openInputStream(uri)
                .use { it?.let { ExifInterface(it) } }

            when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: IOException) {
            e.printStackTrace()
            0f
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun decodeBitmap(uri: Uri): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    }

    private fun getBitmapCompat(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}