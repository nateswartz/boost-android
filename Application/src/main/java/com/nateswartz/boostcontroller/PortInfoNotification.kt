package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

class PortInfoNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "01" -> "C"
        "02" -> "D"
        "37" -> "A"
        "38" -> "B"
        else -> "Unknown"
    }

    val sensor = when (rawData.substring(15, 17)) {
        "17" -> "LED"
        "25" -> "DistanceColor"
        "26" -> "ExternalMotor"
        "27" -> "Motor"
        "28" -> "Tilt"
        else -> "Unknown"
    }

    constructor(parcel: Parcel) : this(parcel.readString()) {
    }

    override fun toString(): String {
        return "Port Info Notification - Port $port - Sensor $sensor - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PortInfoNotification> {
        override fun createFromParcel(parcel: Parcel): PortInfoNotification {
            return PortInfoNotification(parcel)
        }

        override fun newArray(size: Int): Array<PortInfoNotification?> {
            return arrayOfNulls(size)
        }
    }
}