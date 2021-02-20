package com.rosberry.android.gallerypicker.entity

import android.net.Uri

data class GalleryItem(
        override val id: Long,
        val name: String,
        val uri: Uri,
        val selected: Boolean = false
) : MediaItem(id, selected)