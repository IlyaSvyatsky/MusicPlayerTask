package com.example.musicplayer

import android.os.Parcel
import android.os.Parcelable
import android.net.Uri
import android.os.Build

data class MusicList(
    var title: String? = null,
    var artist: String? = null,
    var duration: String? = null,
    var isPlaying: Boolean,
    var musicFile: Uri? = null

): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(Uri::class.java.classLoader)
    ) {
        title = parcel.readString()
        artist = parcel.readString()
        duration = parcel.readString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isPlaying = parcel.readBoolean()
        }
        musicFile = parcel.readParcelable(Uri::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(duration)
        parcel.writeByte(if (isPlaying) 1 else 0)
        parcel.writeParcelable(musicFile, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MusicList> {
        override fun createFromParcel(parcel: Parcel): MusicList {
            return MusicList(parcel)
        }

        override fun newArray(size: Int): Array<MusicList?> {
            return arrayOfNulls(size)
        }
    }

}