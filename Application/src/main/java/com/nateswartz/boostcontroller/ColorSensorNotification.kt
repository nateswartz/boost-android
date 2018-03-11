package com.nateswartz.boostcontroller

class ColorSensorNotification : HubNotification{
    constructor(str: String) : super(str)

    val port = if (rawData[10] == '1') 'C' else 'D'
    val color = getColor("${rawData[12]}${rawData[13]}")
    val distance = getDistance(rawData.substring(15, 17), rawData.substring(21, 23))
    override fun toString(): String {
        return "Color Sensor Notification - Port $port - Color $color - Distance $distance - $rawData"
    }
}

private fun getDistance(inches: String, partial: String): String {
    var result = ""
    result += "${inches.toLong(16)}"
    if (partial != "0") {
        result += ".${partial.toLong(16)}"
    }
    result += " inches"
    return result
}
