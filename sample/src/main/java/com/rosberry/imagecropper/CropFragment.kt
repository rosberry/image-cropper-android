package com.rosberry.imagecropper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rosberry.imagecropper.databinding.FragmentCropperBinding

class CropFragment : Fragment() {

    private val bitmap
        get() = resources.assets
            .open("image.png")
            .use {
                BitmapFactory.decodeStream(it)
            }

    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCropperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post { binding.cropView.setBitmap(bitmap) }
    }

    fun getCroppedImage(): Bitmap? {
        return binding.cropView.getCroppedImage()
    }
}