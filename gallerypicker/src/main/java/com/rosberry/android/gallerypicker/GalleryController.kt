package com.rosberry.android.gallerypicker

import com.rosberry.android.gallerypicker.entity.MediaItem

interface GalleryController {

    fun selectItem(item: MediaItem)

    fun toggleSelection(item: MediaItem): Boolean {
        return false
    }
}