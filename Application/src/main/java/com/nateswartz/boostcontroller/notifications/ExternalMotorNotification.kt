package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort

class ExternalMotorNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = if (rawData[10] == '1') BoostPort.C else BoostPort.D

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "External Motor Notification - Port $port - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExternalMotorNotification> {
        override fun createFromParcel(parcel: Parcel): ExternalMotorNotification {
            return ExternalMotorNotification(parcel)
        }

        override fun newArray(size: Int): Array<ExternalMotorNotification?> {
            return arrayOfNulls(size)
        }
    }
}
