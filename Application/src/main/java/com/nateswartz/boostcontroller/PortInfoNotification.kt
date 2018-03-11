package com.nateswartz.boostcontroller

class PortInfoNotification : HubNotification{
    constructor(str: String) : super(str)

    val port = when (rawData.substring(9, 11)) {
        "01" -> "C"
        "02" -> "D"
        "37" -> "A"
        "38" -> "B"
        else -> "Unknown"
    }
    override fun toString(): String {
        return "Port Info Notification - Port $port - $rawData"
    }
}