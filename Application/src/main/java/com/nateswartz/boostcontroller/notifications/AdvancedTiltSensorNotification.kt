package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

class AdvancedTiltSensorNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    val orientation = rawData[4]

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Advanced Tilt Sensor Notification - Orientation ${convertBytesToString(byteArrayOf(orientation))} - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
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
