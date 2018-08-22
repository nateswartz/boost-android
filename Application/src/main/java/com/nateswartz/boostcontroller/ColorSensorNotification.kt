package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

class ColorSensorNotification(private var rawData: String) : HubNotification, Parcelable{
    val port = if (rawData[10] == '1') Port.C else Port.D
    val color = getColorFromHex("${rawData[12]}${rawData[13]}")
    val distance = getDistance(rawData.substring(15, 17), rawData.substring(21, 23))

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Color Sensor Notification - Port $port - Color $color - Distance $distance - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun getDistance(inches: String, partial: String): String {
        var result = "${inches.toLong(16)}"
        if (partial != "0") {
            result += ".${partial.toLong(16)}"
        }
        result += " inches"
        return result
    }

    companion object CREATOR : Parcelable.Creator<ColorSensorNotification> {
        override fun createFromParcel(parcel: Parcel): ColorSensorNotification {
            return ColorSensorNotification(parcel)
        }

        override fun newArray(size: Int): Array<ColorSensorNotification?> {
            return arrayOfNulls(size)
        }
    }
}


