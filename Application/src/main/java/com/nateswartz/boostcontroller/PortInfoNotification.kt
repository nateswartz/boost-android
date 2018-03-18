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

    val sensor = when (rawData.substring(15, 17)) {
        "17" -> "LED"
        "25" -> "DistanceColor"
        "26" -> "ExternalMotor"
        "27" -> "Motor"
        "28" -> "Tilt"
        else -> "Unknown"
    }
    override fun toString(): String {
        return "Port Info Notification - Port $port - Sensor $sensor - $rawData"
    }
}