package com.rosberry.android.imagecropper

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rosberry.android.imagecropper.databinding.FragmentCropperBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class CropFragment : Fragment(), ImageLoadCallback {

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
            cropView.setCallback(this@CropFragment)
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
            view.post { lifecycleScope.launch { withContext(Dispatchers.IO) { cropView.setImageAsset("2053958.jpg") } } }
        }
    }

    suspend fun getCroppedImage(): Bitmap? {
        return withContext(Dispatchers.IO) { binding.cropView.crop() }
    }

    override fun onImageLoaded() {
        binding.cropView.isVisible = true
    }
}