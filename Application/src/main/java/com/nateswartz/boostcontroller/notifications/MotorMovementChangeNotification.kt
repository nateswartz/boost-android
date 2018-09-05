package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.findBoostPort
import com.nateswartz.boostcontroller.misc.convertBytesToString

enum class MotorState(val code: Byte) {
    START(0x01),
    STOP(0x0A),
    NONE(0x00)
}

fun findMotorState(code: Byte): MotorState {
    enumValues<MotorState>().forEach {
        if (it.code == code) {
            return it
        }
    }
    return MotorState.NONE
}

class MotorMovementChangeNotification(private var rawData: ByteArray) : HubNotification, Parcelable{

    val port = findBoostPort(rawData[3])

    val state = findMotorState(rawData[4])

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Motor Movement Change Notification - Port $port - State $state - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
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
