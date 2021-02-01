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
            .use { BitmapFactory.decodeStream(it) }

    private val ratios = listOf(1f, 4/3f, 16/9f, 3/4f, 9/16f)

    private var currentRatio = 0
    private var currentShape = 0

    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCropperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonRatio.setOnClickListener {
            currentRatio = if (currentRatio < ratios.size - 1) currentRatio + 1 else 0
            binding.cropView.frameRatio = ratios[currentRatio]
        }
        binding.buttonShape.setOnClickListener {
            currentShape = if (currentShape < FrameShape.values().size - 1) currentShape + 1 else 0
            binding.cropView.frameShape = FrameShape.values()[currentShape]
        }
        view.post { binding.cropView.setBitmap(bitmap) }
    }

    fun getCroppedImage(): Bitmap? {
        return binding.cropView.getCroppedImage()
    }
}