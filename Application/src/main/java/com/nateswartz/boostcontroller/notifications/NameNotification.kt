package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

class NameNotification(private var rawData: ByteArray) : HubNotification, Parcelable {
    val name = convertBytesToText(rawData.slice(IntRange(5, rawData.size)).toByteArray())

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Name Notification - Name $name - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun convertBytesToText(bytes: ByteArray): String {
        var results = ""
        for (byte in bytes) {
            results += byte.toChar()
        }
        return results
    }

    companion object CREATOR : Parcelable.Creator<NameNotification> {
        override fun createFromParcel(parcel: Parcel): NameNotification {
            return NameNotification(parcel)
        }

        override fun newArray(size: Int): Array<NameNotification?> {
            return arrayOfNulls(size)
        }
    }
}
