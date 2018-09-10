package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

class PongNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Pong Notification - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PongNotification> {
        override fun createFromParcel(parcel: Parcel): PongNotification {
            return PongNotification(parcel)
        }

        override fun newArray(size: Int): Array<PongNotification?> {
            return arrayOfNulls(size)
        }
    }
}
