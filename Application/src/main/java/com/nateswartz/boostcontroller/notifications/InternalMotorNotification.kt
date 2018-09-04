package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.enums.BoostPort

class InternalMotorNotification(private var rawData: String) : HubNotification, Parcelable{

    val port = if (rawData[10] == '7') BoostPort.A else if (rawData[10] == '8') BoostPort.B else BoostPort.A_B
    private val rotation = (rawData.substring(15, 17))
    private val angle = (rawData.substring(12, 14))
    val rotationValue = getRotationValue(rotation, angle)

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Internal Motor Notification - Port $port - Rotation $rotation - Angle $angle - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun getRotationValue(rotation: String, angle: String) : Int {
        var result = 0
        val rotationValue = java.lang.Short.valueOf(rotation,16)
        if (rotationValue > 128) {
            result += ((rotationValue - 256) * 256)
        }

        val angleValue = angle.toLong(16)
        result += angleValue.toInt()
        return result
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
