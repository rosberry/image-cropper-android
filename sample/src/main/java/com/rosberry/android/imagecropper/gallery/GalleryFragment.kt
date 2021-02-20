package com.rosberry.android.imagecropper.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rosberry.android.gallerypicker.GalleryPicker
import com.rosberry.android.imagecropper.MainActivity
import com.rosberry.android.imagecropper.databinding.FragmentGalleryBinding
import com.rosberry.android.imagecropper.result.SampleFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.viewmodel.ext.android.viewModel

class GalleryFragment : SampleFragment() {

    private val readStoragePermission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private val readStoragePermissionCode = 1987

    private val viewModel: GalleryViewModel by viewModel()

    private val galleryPicker by lazy {
        GalleryPicker.Builder(requireContext())
            .setSpanCount(spanCount)
            .setController(viewModel)
            .build()
    }

    private val spanCount
        get() = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 6
            else -> 3
        }

    private lateinit var binding: FragmentGalleryBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if ((ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            viewModel.getMedia()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), readStoragePermission, readStoragePermissionCode)
        }

        lifecycleScope.launch { viewModel.media.collect { galleryPicker.setItems(it) } }
        lifecycleScope.launch { viewModel.isSelecting.collect { galleryPicker.isSelecting = it } }
        lifecycleScope.launch { viewModel.onSelect.collect { (requireActivity() as MainActivity).onImageSelected(it.uri) } }

        galleryPicker.attachToRecyclerView(binding.listMedia)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == readStoragePermissionCode && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.getMedia()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onBackPressed(): Boolean = viewModel.onBackPressed()
}