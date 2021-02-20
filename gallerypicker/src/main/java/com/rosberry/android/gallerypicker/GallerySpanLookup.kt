package com.rosberry.android.gallerypicker

import androidx.recyclerview.widget.GridLayoutManager
import com.rosberry.android.gallerypicker.adapter.GalleryAdapter
import com.rosberry.android.gallerypicker.entity.GalleryItem

class GallerySpanLookup(
        private val adapter: GalleryAdapter,
        private val spanCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        return when (adapter.items[position]) {
            is GalleryItem -> 1
            else -> spanCount
        }
    }
}