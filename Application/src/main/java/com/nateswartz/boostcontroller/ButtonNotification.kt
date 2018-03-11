package com.nateswartz.boostcontroller

class ButtonNotification : HubNotification{
    constructor(str: String) : super(str)
    val buttonState : String = if (rawData[16] == '0') "Released" else "Pressed"

    override fun toString(): String {
        return "Button Change Notification - Button State $buttonState - $rawData"
    }
}
