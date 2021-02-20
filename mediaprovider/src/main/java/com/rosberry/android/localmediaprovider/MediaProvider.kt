/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.localmediaprovider

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.rosberry.android.localmediaprovider.Constant.NO_FOLDER_ID
import com.rosberry.android.localmediaprovider.Constant.NO_LIMIT
import com.rosberry.android.localmediaprovider.sort.SortingMode
import com.rosberry.android.localmediaprovider.sort.SortingOrder

/**
 * @author mmikhailov on 2019-10-31.
 */
class MediaProvider(private val context: Context) {

    private var mediaUpdatesCallback: MediaUpdatesCallback? = null

    private var mediaContentObserver: ContentObserver? = null

    fun getLocalMedia(
            folderId: Long = NO_FOLDER_ID,
            limit: Int = NO_LIMIT,
            filterMode: FilterMode = FilterMode.ALL,
            sortingMode: SortingMode = SortingMode.DATE,
            sortingOrder: SortingOrder = SortingOrder.DESCENDING
    ): List<LocalMedia> {

        val finalFolderId = if (folderId > NO_FOLDER_ID) folderId else NO_FOLDER_ID
        val finalLimit = if (limit > NO_LIMIT) limit else NO_LIMIT

        return queryFromMediaStore(finalFolderId, finalLimit, sortingMode, sortingOrder, filterMode)
    }

    fun registerMediaUpdatesCallback(callback: MediaUpdatesCallback) {
        mediaUpdatesCallback = callback
        registerListener()
    }

    fun unregisterMediaUpdatesCallback() {
        unregisterListener()
    }

    /**
     * Returns list of media refined by query params
     *
     * @param folderId should be {@link MediaProvider.noFolderId} to query all media
     * @param limit should be {@link MediaProvider.noLimit} to query all media
     * @param sortingMode
     * @param sortingOrder
     * @param filterMode
     * */
    private fun queryFromMediaStore(
            folderId: Long,
            limit: Int,
            sortingMode: SortingMode,
            sortingOrder: SortingOrder,
            filterMode: FilterMode
    ): List<LocalMedia> {
        val query = Query.Builder()
            .uri(MediaStore.Files.getContentUri("external"))
            .selection(filterMode.selection(folderId))
            .args(*filterMode.args(folderId))
            .projection(LocalMedia.projection)
            .sort(sortingMode.mediaColumn)
            .ascending(sortingOrder.isAscending)
            .limit(limit)
            .build()

        return query.queryResults(context.contentResolver, CursorHandler { LocalMedia(it) })
    }

    private fun registerListener() {
        mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                mediaUpdatesCallback?.onChange(selfChange)
            }
        }.also { observer ->
            context.contentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, observer)
            context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        }
    }

    private fun unregisterListener() {
        mediaContentObserver?.let { observer ->
            context.contentResolver.unregisterContentObserver(observer)
            mediaContentObserver = null
            mediaUpdatesCallback = null
        }
    }

    private fun <T> Query.queryResults(cr: ContentResolver, ch: CursorHandler<T>): List<T> {
        return getCursor(cr).use { cursor ->
            val result = mutableListOf<T>()
            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) {
                    result.add(ch.handle(cursor))
                }
            }
            result
        }
    }
}