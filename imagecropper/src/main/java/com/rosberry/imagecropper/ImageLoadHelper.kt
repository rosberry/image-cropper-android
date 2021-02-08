package com.rosberry.imagecropper

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class ImageLoadHelper(private val context: Context) {

    fun loadLocalImage(uriString: String): Bitmap {
        return loadLocalImage(Uri.parse(uriString))
    }

    fun loadLocalImage(uri: Uri): Bitmap {
        return when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            true -> decodeBitmap(uri)
            false -> getBitmap(uri)
        }
    }

    fun resizeBitmap(source: Bitmap, rotation: Float? = null): Bitmap {
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
        rotation?.let { matrix.postRotate(it) }

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun decodeBitmap(uri: Uri): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    }

    private fun getBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
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
}