package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.findBoostPort
import com.nateswartz.boostcontroller.misc.convertBytesToString

class ExternalMotorNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    val port = findBoostPort(rawData[3])

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "External Motor Notification - Port $port - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
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
