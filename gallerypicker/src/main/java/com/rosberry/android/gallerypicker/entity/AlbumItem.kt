package com.rosberry.android.gallerypicker.entity

import android.net.Uri

data class AlbumItem(
        override val id: Long,
        val name: String,
        val mediaCount: Int,
        val coverUri: Uri,
        val selected: Boolean = false
) : MediaItem(id, selected)