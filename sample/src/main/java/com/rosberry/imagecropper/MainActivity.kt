package com.rosberry.imagecropper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.rosberry.imagecropper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tagCrop = "crop"

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.buttonApply.setOnClickListener {
            lifecycleScope.launch {
                (supportFragmentManager.findFragmentByTag(tagCrop) as? CropFragment)
                    ?.getCroppedImage()
                    ?.apply {
                        supportFragmentManager
                            .beginTransaction()
                            .replace(binding.appContainer.id, ResultFragment.getInstance(this))
                            .commitNow()

                        binding.buttonBack.isVisible = true
                        binding.buttonApply.isVisible = false
                    }
            }
        }

        binding.buttonBack.setOnClickListener {
            setCropFragment()
        }

        setCropFragment()
    }

    private fun setCropFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.appContainer.id, CropFragment(), tagCrop)
            .commitNow()

        binding.buttonBack.isVisible = false
        binding.buttonApply.isVisible = true
    }
}