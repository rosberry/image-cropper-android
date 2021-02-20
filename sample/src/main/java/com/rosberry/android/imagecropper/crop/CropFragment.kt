package com.rosberry.android.imagecropper.crop

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.rosberry.android.imagecropper.FrameShape
import com.rosberry.android.imagecropper.ImageLoadCallback
import com.rosberry.android.imagecropper.MainActivity
import com.rosberry.android.imagecropper.databinding.FragmentCropperBinding
import com.rosberry.android.imagecropper.result.SampleFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class CropFragment : SampleFragment(), ImageLoadCallback {

    companion object {

        fun getInstance(uri: Uri): CropFragment {
            return CropFragment().apply { arguments = bundleOf("uri" to uri) }
        }
    }

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
            buttonCrop.setOnClickListener {
                lifecycleScope.launch { getCroppedImage() }
            }
            view.post { lifecycleScope.launch { withContext(Dispatchers.IO) { cropView.setImageUri(requireArguments().get("uri") as Uri) } } }
        }
    }

    private suspend fun getCroppedImage() {
        val file = withContext(Dispatchers.IO) {
            val bitmap = binding.cropView.crop() ?: return@withContext null
            val file = File.createTempFile("${System.currentTimeMillis()}", ".jpg")

            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            return@withContext file
        } ?: return

        (requireActivity() as MainActivity).onImageCropped(file)
    }

    override fun onImageLoaded() {
        binding.cropView.isVisible = true
    }
}