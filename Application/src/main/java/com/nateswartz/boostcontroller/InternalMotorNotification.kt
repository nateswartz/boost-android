package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

class InternalMotorNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = if (rawData[10] == '7') 'A' else if (rawData[10] == '8') 'B' else "A + B"
    val rotation = (rawData.substring(15, 17))
    val angle = (rawData.substring(12, 14))

    constructor(parcel: Parcel) : this(parcel.readString()) {
    }

    override fun toString(): String {
        return "Internal Motor Notification - Port $port - Rotation $rotation - Angle $angle - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InternalMotorNotification> {
        override fun createFromParcel(parcel: Parcel): InternalMotorNotification {
            return InternalMotorNotification(parcel)
        }

        override fun newArray(size: Int): Array<InternalMotorNotification?> {
            return arrayOfNulls(size)
        }
    }
}
