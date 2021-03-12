package com.rosberry.android.imagecropper

/**
 * Interface definition for a callback to be invoked when an image is loaded.
 */
interface ImageLoadCallback {

    /**
     * Called after decoded image preview bitmap is set.
     */
    fun onImageLoaded()
}