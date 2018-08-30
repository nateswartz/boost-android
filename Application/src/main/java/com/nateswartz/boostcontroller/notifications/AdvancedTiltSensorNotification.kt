package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable

class AdvancedTiltSensorNotification(private var rawData: String) : HubNotification, Parcelable{

    val orientation = rawData.substring(12, 20)

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Advanced Tilt Sensor Notification - Orientation $orientation - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AdvancedTiltSensorNotification> {
        override fun createFromParcel(parcel: Parcel): AdvancedTiltSensorNotification {
            return AdvancedTiltSensorNotification(parcel)
        }

        override fun newArray(size: Int): Array<AdvancedTiltSensorNotification?> {
            return arrayOfNulls(size)
        }
    }
}
