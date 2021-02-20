package com.rosberry.android.imagecropper

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.rosberry.android.imagecropper.crop.CropFragment
import com.rosberry.android.imagecropper.databinding.ActivityMainBinding
import com.rosberry.android.imagecropper.gallery.GalleryFragment
import com.rosberry.android.imagecropper.result.ResultFragment
import com.rosberry.android.imagecropper.result.SampleFragment
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) setCropFragment()
    }

    fun onImageSelected(uri: Uri) {
        addFragment(CropFragment.getInstance(uri))
    }

    fun onImageCropped(file: File) {
        addFragment(ResultFragment.getInstance(file))
    }

    private fun setCropFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.appContainer.id, GalleryFragment())
            .commitNow()
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.appContainer.id, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        if ((supportFragmentManager.fragments[0] as? SampleFragment)?.onBackPressed() == true) return
        super.onBackPressed()
    }
}