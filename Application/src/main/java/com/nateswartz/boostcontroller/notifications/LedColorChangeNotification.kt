package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

class LedColorChangeNotification(private var rawData: ByteArray) : HubNotification, Parcelable {

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "LED Color Change Notification - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LedColorChangeNotification> {
        override fun createFromParcel(parcel: Parcel): LedColorChangeNotification {
            return LedColorChangeNotification(parcel)
        }

        override fun newArray(size: Int): Array<LedColorChangeNotification?> {
            return arrayOfNulls(size)
        }
    }
}