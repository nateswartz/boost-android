package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.BoostSensor

class PortInfoNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "01" -> BoostPort.C
        "02" -> BoostPort.D
        "37" -> BoostPort.A
        "38" -> BoostPort.B
        else -> BoostPort.NONE
    }

    val sensor = when (rawData.substring(15, 17)) {
        "17" -> BoostSensor.LED
        "25" -> BoostSensor.DISTANCE_COLOR
        "26" -> BoostSensor.EXTERNAL_MOTOR
        "27" -> BoostSensor.MOTOR
        "28" -> BoostSensor.TILT
        else -> BoostSensor.NONE
    }

    constructor(parcel: Parcel) : this(parcel.readString())

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