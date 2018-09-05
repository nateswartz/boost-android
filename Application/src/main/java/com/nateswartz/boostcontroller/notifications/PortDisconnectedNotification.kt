package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostSensor
import com.nateswartz.boostcontroller.enums.findBoostPort
import com.nateswartz.boostcontroller.misc.convertBytesToString

class PortDisconnectedNotification(private var rawData: ByteArray, val sensor: BoostSensor) : HubNotification, Parcelable{

    val port = findBoostPort(rawData[3])

    constructor(parcel: Parcel) : this(parcel.createByteArray(), BoostSensor.valueOf(parcel.readString()))

    override fun toString(): String {
        return "Port Disconnected Notification - Port $port - Sensor ${sensor.name} - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
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