package com.rosberry.android.gallerypicker.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rosberry.android.gallerypicker.GalleryController
import com.rosberry.android.gallerypicker.R
import com.rosberry.android.gallerypicker.entity.AlbumItem
import com.squareup.picasso.Picasso

class AlbumViewHolder(
        view: View,
        controller: GalleryController? = null
) : RecyclerView.ViewHolder(view) {

    private val textName = itemView.findViewById<TextView?>(R.id.textName)
    private val imageCover = itemView.findViewById<ImageView?>(R.id.imageCover)
    private val textCount = itemView.findViewById<TextView?>(R.id.textCount)

    private lateinit var item: AlbumItem

    init {
        itemView.setOnClickListener { controller?.selectItem(item) }
    }

    fun bind(item: AlbumItem) {
        this.item = item

        textName?.text = item.name
        textCount?.text = "${item.mediaCount}"
        imageCover?.let {
            Picasso.get()
                .load(item.coverUri)
                .centerCrop()
                .fit()
                .into(it)
        }
    }
}