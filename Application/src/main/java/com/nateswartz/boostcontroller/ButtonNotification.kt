package com.nateswartz.boostcontroller

import android.os.Parcel
import android.os.Parcelable

enum class ButtonState {
    RELEASED, PRESSED
}

class ButtonNotification(private var rawData: String) : HubNotification, Parcelable {
    val buttonState : ButtonState = if (rawData[16] == '0') ButtonState.RELEASED else ButtonState.PRESSED

    constructor(parcel: Parcel) : this(parcel.readString())

    override fun toString(): String {
        return "Button Change Notification - Button State $buttonState - $rawData"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rawData)
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
