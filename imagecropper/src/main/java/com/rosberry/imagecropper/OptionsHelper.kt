package com.rosberry.imagecropper

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream

object OptionsHelper {

    fun getAssetOptions(context: Context, fileName: String): BitmapFactory.Options {
        return decodeOptions(context.resources.assets.open(fileName))
    }

    fun getFileOptions(context: Context, uri: Uri): BitmapFactory.Options {
        return decodeOptions(context.contentResolver.openInputStream(uri))
    }

    fun getResourceOptions(context: Context, resId: Int): BitmapFactory.Options {
        val options = BitmapFactory.Options()
            .apply { inJustDecodeBounds = true }

        BitmapFactory.decodeResource(context.resources, resId, options)
        return options
    }

    private fun decodeOptions(inputStream: InputStream?): BitmapFactory.Options {
        val options = BitmapFactory.Options()
            .apply { inJustDecodeBounds = true }

        inputStream.use { BitmapFactory.decodeStream(it, null, options) }
        return options
    }
}