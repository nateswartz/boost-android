package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

interface HubNotification : Parcelable{
    override fun toString(): String
}

open class UnknownHubNotification(var rawData: ByteArray) : HubNotification, Parcelable{
    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Hub Notification - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UnknownHubNotification> {
        override fun createFromParcel(parcel: Parcel): UnknownHubNotification {
            return UnknownHubNotification(parcel)
        }

        override fun newArray(size: Int): Array<UnknownHubNotification?> {
            return arrayOfNulls(size)
        }
    }
}
