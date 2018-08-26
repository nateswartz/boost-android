package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

class LedColorChangeNotification(private var rawData: String) : HubNotification, Parcelable {
    val color =  getColorFromHex(rawData.substring(12, 14))

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "LED Color Change Notification - Color $color - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
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