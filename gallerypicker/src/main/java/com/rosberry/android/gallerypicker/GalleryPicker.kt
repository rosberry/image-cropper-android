package com.rosberry.android.gallerypicker

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rosberry.android.gallerypicker.adapter.GalleryAdapter
import com.rosberry.android.gallerypicker.entity.MediaItem

open class GalleryPicker private constructor(
        private val adapter: GalleryAdapter,
        private val spanCount: Int
) {

    var isSelecting = false
        set(value) {
            adapter.isSelecting = value
            field = value
        }

    fun setItems(items: List<MediaItem>) {
        adapter.setItems(items)
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = GridLayoutManager(recyclerView.context, spanCount).apply { spanSizeLookup = GallerySpanLookup(adapter, spanCount) }
        recyclerView.adapter = adapter
    }

    class Builder(private val context: Context) {

        private var spanCount: Int = 3

        private var controller: GalleryController? = null

        @LayoutRes
        private var albumLayoutId: Int = R.layout.item_album

        @LayoutRes
        private var galleryLayoutId: Int = R.layout.item_gallery

        fun setSpanCount(spanCount: Int): Builder {
            this.spanCount = spanCount
            return this
        }

        fun setController(controller: GalleryController): Builder {
            this.controller = controller
            return this
        }

        fun setAlbumLayoutId(albumLayoutId: Int): Builder {
            this.albumLayoutId = albumLayoutId
            return this
        }

        fun setGalleryLayoutId(galleryLayoutId: Int): Builder {
            this.galleryLayoutId = galleryLayoutId
            return this
        }

        fun build(): GalleryPicker {
            val adapter = GalleryAdapter(albumLayoutId, galleryLayoutId, controller)
            return GalleryPicker(adapter, spanCount)
        }
    }
}