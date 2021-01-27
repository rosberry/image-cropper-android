package com.rosberry.imagecropper

import android.graphics.Bitmap

interface CropListener {
    fun onImageLoaded()
    fun onImageLoadError()
    fun onImageCrop(bitmap: Bitmap)
    fun onImageCropError()
}