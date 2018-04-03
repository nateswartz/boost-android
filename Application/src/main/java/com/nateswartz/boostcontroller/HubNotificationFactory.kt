package com.nateswartz.boostcontroller

object HubNotificationFactory {

    var ColorSensorPort = ""
    var ExternalMotorPort = ""

    fun build(byteData: ByteArray): HubNotification {
        return build(convertBytesToString(byteData))
    }

    fun build(stringData: String): HubNotification {
        if (stringData.startsWith("06 00 01 02 06 ")) {
            return ButtonNotification(stringData)
        } else if (stringData.startsWith("05 00 82 32")) {
            return LedColorChangeNotification(stringData)
        } else if (stringData.substring(9, 11) == "01" && ColorSensorPort == "C"
                || stringData.substring(9, 11) == "02" && ColorSensorPort == "D") {
            return ColorSensorNotification(stringData)
        } else if (stringData.substring(9, 11) == "01" && ExternalMotorPort == "C"
                || stringData.substring(9, 11) == "02" && ExternalMotorPort == "D") {
            return ExternalMotorNotification(stringData)
        } else if (stringData.startsWith("0F 00 04")) {
            return PortInfoNotification(stringData)
        }
        return UnknownHubNotification(stringData)
    }

    private fun convertBytesToString(bytes: ByteArray): String {
        var result = ""
        for (b in bytes) {
            result += String.format("%02X", b)
        }
        return result
    }
}