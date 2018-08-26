package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

enum class Orientation {
    FLAT,
    STANDING_LED_UP,
    STANDING_BUTTON_UP,
    B_D_UP,
    A_C_UP,
    BATTERIES_UP,
    UNKNOWN
}

class TiltSensorNotification(private var rawData: String) : HubNotification, Parcelable{

    val orientation = when (rawData.substring(12, 14)) {
        "00" -> Orientation.FLAT
        "01" -> Orientation.STANDING_LED_UP
        "02" -> Orientation.STANDING_BUTTON_UP
        "03" -> Orientation.B_D_UP
        "04" -> Orientation.A_C_UP
        "05" -> Orientation.BATTERIES_UP
        else -> Orientation.UNKNOWN
    }

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Tilt Sensor Notification - Orientation ${orientation.name} - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
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
