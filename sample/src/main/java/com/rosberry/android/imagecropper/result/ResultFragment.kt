package com.rosberry.android.imagecropper.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.rosberry.android.imagecropper.databinding.FragmentResultBinding
import com.squareup.picasso.Picasso
import java.io.File

class ResultFragment : SampleFragment() {

    companion object {

        fun getInstance(file: File): ResultFragment {
            return ResultFragment().apply { arguments = bundleOf("file" to file) }
        }
    }

    private lateinit var binding: FragmentResultBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResultBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Picasso.get()
            .load(requireArguments().get("file") as File)
            .into(binding.resultView)
    }
}