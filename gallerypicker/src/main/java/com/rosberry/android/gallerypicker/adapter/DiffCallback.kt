package com.rosberry.android.gallerypicker.adapter

import androidx.recyclerview.widget.DiffUtil
import com.rosberry.android.gallerypicker.entity.MediaItem

class DiffCallback(
        private val oldItems: List<MediaItem>,
        private val newItems: List<MediaItem>
) : DiffUtil.Callback() {

    object SelectionChange

    object SelectedChange

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.id == newItem.id
    }

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]

        return if (oldItem.isSelected != newItem.isSelected) SelectedChange
        else super.getChangePayload(oldItemPosition, newItemPosition)
    }
}