package com.rosberry.android.gallerypicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rosberry.android.gallerypicker.GalleryController
import com.rosberry.android.gallerypicker.entity.AlbumItem
import com.rosberry.android.gallerypicker.entity.GalleryItem
import com.rosberry.android.gallerypicker.entity.MediaItem

private const val ITEM_GALLERY = 0
private const val ITEM_ALBUM = 1

class GalleryAdapter(
        private val albumResId: Int,
        private val galleryResId: Int,
        private val controller: GalleryController? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isSelecting = false
        set(value) {
            notifyItemRangeChanged(0, items.size, DiffCallback.SelectionChange)
            field = value
        }

    var items: List<MediaItem> = listOf()
        private set

    fun setItems(newItems: List<MediaItem>) {
        DiffCallback(items, newItems)
            .run { DiffUtil.calculateDiff(this) }
            .also { items = newItems }
            .dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GalleryItem -> ITEM_GALLERY
            is AlbumItem -> ITEM_ALBUM
            else -> throw IllegalStateException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_GALLERY -> GalleryViewHolder(LayoutInflater.from(parent.context)
                .inflate(galleryResId, parent, false), controller)

            ITEM_ALBUM -> AlbumViewHolder(LayoutInflater.from(parent.context)
                .inflate(albumResId, parent, false), controller)

            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (getItemViewType(position) == ITEM_GALLERY) {
            when {
                payloads.contains(DiffCallback.SelectionChange) -> (holder as GalleryViewHolder).bindSelecting(items[position] as GalleryItem, isSelecting)
                payloads.contains(DiffCallback.SelectedChange) -> (holder as GalleryViewHolder).bindSelection(items[position] as GalleryItem)
                else -> super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_GALLERY -> (holder as GalleryViewHolder).bind(items[position] as GalleryItem, isSelecting)
            ITEM_ALBUM -> (holder as AlbumViewHolder).bind(items[position] as AlbumItem)
        }
    }
}