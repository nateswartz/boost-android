package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.BoostSensor

class PortDisconnectedNotification(private var rawData: String, val sensor: BoostSensor) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "01" -> BoostPort.C
        "02" -> BoostPort.D
        "37" -> BoostPort.A
        "38" -> BoostPort.B
        else -> BoostPort.NONE
    }

    constructor(parcel: Parcel) : this(parcel.readString(), BoostSensor.valueOf(parcel.readString()))

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