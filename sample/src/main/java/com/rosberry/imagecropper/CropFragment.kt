package com.rosberry.imagecropper

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rosberry.imagecropper.databinding.FragmentCropperBinding
import java.io.FileNotFoundException
import java.util.Random

class CropFragment : Fragment() {

    private val random by lazy { Random() }

    private val ratios = listOf(1f, 4 / 3f, 16 / 9f, 3 / 4f, 9 / 16f)

    private var currentRatio = 0
    private var currentShape = 0

    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCropperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            buttonColor.setOnClickListener {
                cropView.overlayColor = Color.argb(127, random.nextInt(256), random.nextInt(256), random.nextInt(256))
                cropView.frameColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                cropView.gridColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            }
            buttonRatio.setOnClickListener {
                currentRatio = if (currentRatio < ratios.size - 1) currentRatio + 1 else 0
                cropView.frameRatio = ratios[currentRatio]
            }
            buttonShape.setOnClickListener {
                currentShape = if (currentShape < FrameShape.values().size - 1) currentShape + 1 else 0
                cropView.frameShape = FrameShape.values()[currentShape]
            }
            buttonGrid.setOnClickListener {
                cropView.gridRows = random.nextInt(67) + 3
            }
            view.post { cropView.setImageAsset("2053958.jpg") }
        }
    }

    fun getCroppedImage(): Bitmap? {
        return binding.cropView.crop()
    }
}