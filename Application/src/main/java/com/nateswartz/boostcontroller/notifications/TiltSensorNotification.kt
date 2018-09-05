package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.findTiltSensorOrientation
import com.nateswartz.boostcontroller.misc.convertBytesToString


class TiltSensorNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    val orientation = findTiltSensorOrientation(rawData[4])

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Tilt Sensor Notification - Orientation ${orientation.name} - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TiltSensorNotification> {
        override fun createFromParcel(parcel: Parcel): TiltSensorNotification {
            return TiltSensorNotification(parcel)
        }

        override fun newArray(size: Int): Array<TiltSensorNotification?> {
            return arrayOfNulls(size)
        }
    }
}
