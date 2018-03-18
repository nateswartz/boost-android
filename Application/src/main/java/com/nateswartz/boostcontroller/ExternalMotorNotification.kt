package com.nateswartz.boostcontroller

class ExternalMotorNotification : HubNotification{
    constructor(str: String) : super(str)

    val port = if (rawData[10] == '1') 'C' else 'D'
    override fun toString(): String {
        return "External Motor Notification - Port $port - $rawData"
    }
}
