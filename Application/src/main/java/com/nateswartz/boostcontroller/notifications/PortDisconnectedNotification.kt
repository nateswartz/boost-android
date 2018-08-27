package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable

class PortDisconnectedNotification(private var rawData: String, val sensor: Sensor) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "01" -> Port.C
        "02" -> Port.D
        "37" -> Port.A
        "38" -> Port.B
        else -> Port.UNKNOWN
    }

    constructor(parcel: Parcel) : this(parcel.readString(), Sensor.valueOf(parcel.readString()))

    override fun toString(): String {
        return "Port Disconnected Notification - Port $port - Sensor ${sensor.name} - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
        parcel.writeString(sensor.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PortDisconnectedNotification> {
        override fun createFromParcel(parcel: Parcel): PortDisconnectedNotification {
            return PortDisconnectedNotification(parcel)
        }

        override fun newArray(size: Int): Array<PortDisconnectedNotification?> {
            return arrayOfNulls(size)
        }
    }
}