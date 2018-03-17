package com.nateswartz.boostcontroller

class IMotorNotification : HubNotification{
    constructor(str: String) : super(str)

    val port = if (rawData[10] == '1') 'C' else 'D'
    override fun toString(): String {
        return "IMotor Notification - Port $port - $rawData"
    }
}
