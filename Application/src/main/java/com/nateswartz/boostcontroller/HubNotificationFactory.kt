package com.nateswartz.boostcontroller

object HubNotificationFactory {
    fun build(byteData: ByteArray): HubNotification {
        return build(convertBytesToString(byteData))
    }

    fun build(stringData: String): HubNotification {
        if (stringData.startsWith("06 00 01 02 06 ")) {
            return ButtonNotification(stringData)
        } else if (stringData.startsWith("05 00 82 32")) {
            return LedColorChangeNotification(stringData)
        } else if (stringData.startsWith("08 00 45")) {
            return ColorSensorNotification(stringData)
        } else if (stringData.startsWith("0F 00 04")) {
            return PortInfoNotification(stringData)
        }
        return HubNotification(stringData)
    }

    private fun convertBytesToString(bytes: ByteArray): String {
        var result = ""
        for (b in bytes) {
            result += String.format("%02X", b)
        }
        return result
    }
}