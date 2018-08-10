package com.nateswartz.boostcontroller

object HubNotificationFactory {

    var ColorSensorPort = Port.UNKNOWN
    var ExternalMotorPort = Port.UNKNOWN

    fun build(byteData: ByteArray): HubNotification {
        return build(convertBytesToString(byteData))
    }

    private fun build(stringData: String): HubNotification {
        if (stringData.startsWith("06 00 01 02 06 ")) {
            return ButtonNotification(stringData)
        } else if (stringData.startsWith("05 00 82 32")) {
            return LedColorChangeNotification(stringData)
        } else if (stringData.startsWith("08 00 45")) {
            return InternalMotorNotification(stringData)
        } else if (stringData.substring(9, 11) == "01" && ColorSensorPort == Port.C
                || stringData.substring(9, 11) == "02" && ColorSensorPort == Port.D) {
            return ColorSensorNotification(stringData)
        } else if (stringData.substring(9, 11) == "01" && ExternalMotorPort == Port.C
                || stringData.substring(9, 11) == "02" && ExternalMotorPort == Port.D) {
            return ExternalMotorNotification(stringData)
        } else if (stringData.startsWith("0F 00 04")) {
            return PortInfoNotification(stringData)
        }
        return UnknownHubNotification(stringData)
    }

    private fun convertBytesToString(bytes: ByteArray): String {
        val stringBuilder = StringBuilder(bytes.size)
        for (byteChar in bytes)
            stringBuilder.append(String.format("%02X ", byteChar))
        val pieces = (String(bytes) + "\n" + stringBuilder.toString()).split("\n")
        val encodedData = pieces[pieces.size - 1].trim()
        return encodedData
    }
}