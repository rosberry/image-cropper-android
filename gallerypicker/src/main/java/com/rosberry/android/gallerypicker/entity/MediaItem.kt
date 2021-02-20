package com.rosberry.android.gallerypicker.entity

import java.io.Serializable

open class MediaItem(
        open val id: Long,
        val isSelected: Boolean
) : Serializable