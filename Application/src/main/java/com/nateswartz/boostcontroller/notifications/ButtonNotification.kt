package com.nateswartz.boostcontroller.notifications

import android.os.Parcel
import android.os.Parcelable
import com.nateswartz.boostcontroller.misc.convertBytesToString

enum class ButtonState {
    RELEASED, PRESSED
}

class ButtonNotification(private var rawData: ByteArray) : HubNotification, Parcelable {
    val buttonState : ButtonState = if (rawData[5] == 0x00.toByte()) ButtonState.RELEASED else ButtonState.PRESSED

    constructor(parcel: Parcel) : this(parcel.createByteArray())

    override fun toString(): String {
        return "Button Change Notification - Button State $buttonState - ${convertBytesToString(rawData)}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(rawData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ButtonNotification> {
        override fun createFromParcel(parcel: Parcel): ButtonNotification {
            return ButtonNotification(parcel)
        }

        override fun newArray(size: Int): Array<ButtonNotification?> {
            return arrayOfNulls(size)
        }
    }
}
