package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.findBoostPort
import com.nateswartz.boostcontroller.misc.convertBytesToString
import com.nateswartz.boostcontroller.misc.getColorFromHex

class ColorSensorNotification(private var rawData: ByteArray) : HubNotification, Parcelable{
    val port = findBoostPort(rawData[3])
    val color = getColorFromHex(rawData[4])
    val distance = getDistance(rawData[5], rawData[7])

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Color Sensor Notification - Port $port - Color $color - Distance $distance - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun getDistance(inches: Byte, partial: Byte): String {
        var result = "${inches.toLong()}"
        if (partial.toLong() != 0.toLong()) {
            result += ".${partial.toLong()}"
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


