package com.rosberry.android.imagecropper.gallery

import com.rosberry.android.gallerypicker.entity.AlbumItem
import com.rosberry.android.gallerypicker.entity.GalleryItem
import com.rosberry.android.localmediaprovider.LocalMedia

object GalleryMapper {

    fun mapGalleryItem(items: List<LocalMedia>, selectedIds: Set<Long>, album: String): List<GalleryItem> {
        return items.asSequence()
            .filter { it.folderName == album }
            .map { GalleryItem(it.id, it.fileName, it.uri, selectedIds.contains(it.id)) }
            .toList()
    }

    fun mapAlbumItem(items: List<LocalMedia>): List<AlbumItem> {
        if (items.isNullOrEmpty()) return emptyList()

        val groups = items.groupBy { it.folderName }
        val albums = mutableListOf<AlbumItem>()

        for (i in 0 until groups.size) {
            albums.add(AlbumItem(
                    i.toLong(),
                    groups.keys.elementAt(i),
                    groups.values.elementAt(i).size,
                    groups.values.elementAt(i)[0].uri
            ))
        }
        return albums
    }
}