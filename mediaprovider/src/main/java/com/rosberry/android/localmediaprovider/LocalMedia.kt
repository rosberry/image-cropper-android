/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.localmediaprovider

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore

class LocalMedia : Parcelable {

    companion object {

        private const val FOLDER_NAME_ROOT = "/"
        private const val FOLDER_NAME_0 = "0"

        val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.ORIENTATION,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.PARENT,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )

        @JvmField
        val CREATOR: Parcelable.Creator<LocalMedia> = object : Parcelable.Creator<LocalMedia> {
            override fun createFromParcel(source: Parcel): LocalMedia {
                return LocalMedia(source)
            }

            override fun newArray(size: Int): Array<LocalMedia?> {
                return arrayOfNulls(size)
            }
        }
    }

    val fileName: String
    val mimeType: String
    val fileSize: Long
    val width: Int
    val height: Int
    val orientation: Int
    val dateModified: Long
    val folderId: Long
    val folderName: String
    val uri: Uri
    val id: Long

    constructor(cur: Cursor) {
        this.id = cur.getLong(projection.indexOf(MediaStore.Files.FileColumns._ID))
        this.fileName = cur.getString(projection.indexOf(MediaStore.Files.FileColumns.DISPLAY_NAME)) ?: this.id.toString()
        this.mimeType = cur.getString(projection.indexOf(MediaStore.Files.FileColumns.MIME_TYPE))
        this.fileSize = cur.getLong(projection.indexOf(MediaStore.Files.FileColumns.SIZE))
        this.width = cur.getInt(projection.indexOf(MediaStore.Files.FileColumns.WIDTH))
        this.height = cur.getInt(projection.indexOf(MediaStore.Files.FileColumns.HEIGHT))
        this.orientation = cur.getInt(projection.indexOf(MediaStore.Files.FileColumns.ORIENTATION))
        this.dateModified = cur.getLong(projection.indexOf(MediaStore.Files.FileColumns.DATE_TAKEN))
        this.folderId = cur.getLong(projection.indexOf(MediaStore.Files.FileColumns.PARENT))
        this.uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)

        val folderName = cur.getString(projection.indexOf(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME))
        this.folderName = if (folderName == null || folderName == FOLDER_NAME_0) FOLDER_NAME_ROOT else folderName
    }

    constructor(`in`: Parcel) {
        this.id = `in`.readLong()
        this.fileName = `in`.readString()!!
        this.mimeType = `in`.readString()!!
        this.fileSize = `in`.readLong()
        this.width = `in`.readInt()
        this.height = `in`.readInt()
        this.orientation = `in`.readInt()
        this.dateModified = `in`.readLong()
        this.folderId = `in`.readLong()
        this.folderName = `in`.readString()!!
        this.uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
    }

    override fun equals(other: Any?) = other is LocalMedia && uri == other.uri

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + orientation
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun toString(): String {
        return """LocalMedia(
                id: $id,
                fileName: $fileName,
                mimeType: $mimeType,
                fileSize: $fileSize,
                width: $width,
                height: $height,
                orientation: $orientation,
                dateModified: $dateModified,
                folderId: $folderId,
                folderName: $folderName,
                uri: $uri
            )""".trimIndent()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(this.id)
        dest.writeString(this.fileName)
        dest.writeString(this.mimeType)
        dest.writeLong(this.fileSize)
        dest.writeInt(this.width)
        dest.writeInt(this.height)
        dest.writeInt(this.orientation)
        dest.writeLong(this.dateModified)
        dest.writeLong(this.folderId)
        dest.writeString(this.folderName)
    }
}