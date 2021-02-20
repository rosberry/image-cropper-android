package com.rosberry.android.imagecropper.result

import androidx.fragment.app.Fragment

abstract class SampleFragment : Fragment() {

    open fun onBackPressed(): Boolean = false
}