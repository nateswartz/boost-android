package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.findBoostPort
import com.nateswartz.boostcontroller.enums.findBoostSensor
import com.nateswartz.boostcontroller.misc.convertBytesToString

class PortInfoNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    val port = findBoostPort(rawData[3])

    val sensor = findBoostSensor(rawData[5])

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Port Info Notification - Port $port - Sensor $sensor - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
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