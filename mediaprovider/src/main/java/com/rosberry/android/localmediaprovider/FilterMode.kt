package com.rosberry.android.localmediaprovider

import android.provider.MediaStore

/**
 * @author mmikhailov on 2019-11-06.
 */
enum class FilterMode {

    ALL {

        override fun args(folderId: Long): Array<Any> {
            return if (folderId.isValid()) {
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        folderId)
            } else {
                arrayOf(
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                )
            }
        }

        override fun selection(folderId: Long): String {
            return if (folderId.isValid()) {
                String.format("(%s=? or %s=?) and %s=?",
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.PARENT)
            } else {
                String.format("%s=? or %s=?",
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.MEDIA_TYPE)
            }
        }
    },

    IMAGES {

        override fun args(folderId: Long): Array<Any> {
            return if (folderId.isValid()) {
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, folderId)
            } else {
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            }
        }
    },

    VIDEO {

        override fun args(folderId: Long): Array<Any> {
            return if (folderId.isValid()) {
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, folderId)
            } else {
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            }
        }
    };

    abstract fun args(folderId: Long): Array<Any>

    open fun selection(folderId: Long): String {
        return if (folderId.isValid()) {
            String.format("%s=? and %s=?",
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.PARENT)
        } else {
            String.format("%s=?", MediaStore.Files.FileColumns.MEDIA_TYPE)
        }
    }

    protected fun Long.isValid(): Boolean = this > Constant.NO_FOLDER_ID
}