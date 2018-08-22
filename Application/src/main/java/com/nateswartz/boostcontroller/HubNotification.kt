package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

interface HubNotification : Parcelable{
    override fun toString(): String
}

open class UnknownHubNotification(var rawData: String) : HubNotification, Parcelable{
    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Hub Notification - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
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
