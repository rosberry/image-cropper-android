package com.rosberry.imagecropper

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rosberry.imagecropper.databinding.FragmentResultBinding

class ResultFragment private constructor() : Fragment() {

    companion object {

        fun getInstance(bitmap: Bitmap): ResultFragment {
            return ResultFragment().apply { this.bitmap = bitmap }
        }
    }

    private lateinit var bitmap: Bitmap
    private lateinit var binding: FragmentResultBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentResultBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.resultView.setImageBitmap(bitmap)
    }
}