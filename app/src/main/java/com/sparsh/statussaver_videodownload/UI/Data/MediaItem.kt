package com.sparsh.statussaver_videodownload.UI.Data

import android.os.Parcel
import android.os.Parcelable

data class MediaItem(
    val id: Long,
    val displayName: String,
    val data: String,
    val isVideo: Boolean // Add this property
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte() // Read the isVideo property from the Parcel
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(displayName)
        parcel.writeString(data)
        parcel.writeByte(if (isVideo) 1 else 0) // Write the isVideo property to the Parcel
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaItem> {
        override fun createFromParcel(parcel: Parcel): MediaItem {
            return MediaItem(parcel)
        }

        override fun newArray(size: Int): Array<MediaItem?> {
            return arrayOfNulls(size)
        }
    }
}
