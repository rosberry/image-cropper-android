package com.rosberry.android.imagecropper.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosberry.android.gallerypicker.GalleryController
import com.rosberry.android.gallerypicker.entity.AlbumItem
import com.rosberry.android.gallerypicker.entity.GalleryItem
import com.rosberry.android.gallerypicker.entity.MediaItem
import com.rosberry.android.localmediaprovider.FilterMode
import com.rosberry.android.localmediaprovider.LocalMedia
import com.rosberry.android.localmediaprovider.MediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(
        private val mediaProvider: MediaProvider
) : ViewModel(), GalleryController {

    val media: Flow<List<MediaItem>>
        get() = combine(mediaState, albumsState, selectedState) { media, album, selectedIds ->
            when (album == null) {
                true -> GalleryMapper.mapAlbumItem(media)
                false -> GalleryMapper.mapGalleryItem(media, selectedIds, album)
            }
        }

    val isSelecting: Flow<Boolean>
        get() = selectingState

    private val mediaState = MutableStateFlow(emptyList<LocalMedia>())
    private val albumsState = MutableStateFlow<String?>(null)
    private val selectingState = MutableStateFlow(false)
    private val selectedState = MutableStateFlow(setOf<Long>())
    private val onSelectChannel = Channel<GalleryItem>(Channel.BUFFERED)

    val onSelect: Flow<GalleryItem>
        get() = onSelectChannel.receiveAsFlow()

    fun getMedia() {
        viewModelScope.launch {
            mediaState.value = withContext(Dispatchers.IO) { mediaProvider.getLocalMedia(filterMode = FilterMode.IMAGES) }
        }
    }

    fun onBackPressed(): Boolean {
        return when {
            selectingState.value -> {
                selectingState.value = false
                selectedState.value = setOf()
                true
            }
            albumsState.value != null -> {
                albumsState.value = null
                true
            }
            else -> false
        }
    }

    override fun selectItem(item: MediaItem) {
        viewModelScope.launch {
            when (item) {
                is GalleryItem -> selectMedia(item)
                is AlbumItem -> selectAlbum(item)
            }
        }
    }

    private fun selectMedia(item: GalleryItem) {
        viewModelScope.launch {
            if (selectingState.value) {
                val selected = selectedState.value.toMutableSet()

                if (selected.contains(item.id)) selected.remove(item.id)
                else selected.add(item.id)

                selectedState.value = selected
            } else {
                onSelectChannel.send(item)
            }
        }
    }

    private fun selectAlbum(item: AlbumItem) {
        albumsState.value = item.name
    }

    override fun toggleSelection(item: MediaItem): Boolean = false
}