package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.TiltSensorOrientation


class TiltSensorNotification(private var rawData: String) : HubNotification, Parcelable{

    val orientation = when (rawData.substring(12, 14)) {
        "00" -> TiltSensorOrientation.FLAT
        "01" -> TiltSensorOrientation.STANDING_LED_UP
        "02" -> TiltSensorOrientation.STANDING_BUTTON_UP
        "03" -> TiltSensorOrientation.B_D_UP
        "04" -> TiltSensorOrientation.A_C_UP
        "05" -> TiltSensorOrientation.BATTERIES_UP
        else -> TiltSensorOrientation.UNKNOWN
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
