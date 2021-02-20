package com.rosberry.android.gallerypicker.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rosberry.android.gallerypicker.GalleryController
import com.rosberry.android.gallerypicker.R
import com.rosberry.android.gallerypicker.entity.GalleryItem
import com.squareup.picasso.Picasso

class GalleryViewHolder(
        view: View,
        controller: GalleryController? = null
) : RecyclerView.ViewHolder(view) {

    private val textName = itemView.findViewById<TextView?>(R.id.textName)
    private val image = itemView.findViewById<ImageView?>(R.id.imageMedia)
    private val selection = itemView.findViewById<View>(R.id.indicatorSelection)

    private lateinit var item: GalleryItem

    init {
        controller?.apply {
            itemView.setOnClickListener { selectItem(item) }
            itemView.setOnLongClickListener { toggleSelection(item) }
        }
    }

    fun bind(item: GalleryItem, isSelecting: Boolean) {
        this.item = item

        textName?.text = item.name
        selection?.visibility = if (isSelecting) View.VISIBLE else View.GONE
        selection?.isSelected = item.isSelected
        image?.let {
            Picasso.get()
                .load(item.uri)
                .centerCrop()
                .fit()
                .into(it)
        }
    }

    fun bindSelecting(item: GalleryItem, isSelecting: Boolean) {
        selection?.visibility = if (isSelecting) View.VISIBLE else View.GONE
        selection?.isSelected = item.isSelected
    }

    fun bindSelection(item: GalleryItem) {
        selection?.isSelected = item.isSelected
    }
}