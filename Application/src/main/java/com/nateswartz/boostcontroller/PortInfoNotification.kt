package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

enum class Port {
    A, B, A_B, C, D, UNKNOWN
}

enum class Sensor {
    LED, DISTANCE_COLOR, EXTERNAL_MOTOR, MOTOR, TILT, UNKNOWN
}

class PortInfoNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "01" -> Port.C
        "02" -> Port.D
        "37" -> Port.A
        "38" -> Port.B
        else -> Port.UNKNOWN
    }

    val sensor = when (rawData.substring(15, 17)) {
        "17" -> Sensor.LED
        "25" -> Sensor.DISTANCE_COLOR
        "26" -> Sensor.EXTERNAL_MOTOR
        "27" -> Sensor.MOTOR
        "28" -> Sensor.TILT
        else -> Sensor.UNKNOWN
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