package com.rosberry.imagecropper

import android.graphics.Bitmap
import android.graphics.PointF

internal object CropHelper {

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
}