package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort

enum class MotorState {
    START, STOP, NONE
}

class MotorMovementChangeNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = when (rawData.substring(9, 11)) {
        "37" -> BoostPort.A
        "38" -> BoostPort.B
        "39" -> BoostPort.A_B
        else -> BoostPort.NONE
    }

    val state = when (rawData.substring(12, 14)) {
        "01" -> MotorState.START
        "0A" -> MotorState.STOP
        else -> MotorState.NONE
    }

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Motor Movement Change Notification - Port $port - State $state - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MotorMovementChangeNotification> {
        override fun createFromParcel(parcel: Parcel): MotorMovementChangeNotification {
            return MotorMovementChangeNotification(parcel)
        }

        override fun newArray(size: Int): Array<MotorMovementChangeNotification?> {
            return arrayOfNulls(size)
        }
    }
}
