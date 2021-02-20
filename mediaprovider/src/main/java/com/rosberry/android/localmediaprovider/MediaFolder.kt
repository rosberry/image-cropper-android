package com.rosberry.android.localmediaprovider

/**
 * @author mmikhailov on 2019-11-14.
 */
data class MediaFolder(
        val id: Long,
        val name: String,
        val dateModified: Long,
        val mediaCount: Int
)