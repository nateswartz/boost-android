package com.nateswartz.boostcontroller

object HubNotificationFactory {

    var ColorSensorPort = Port.UNKNOWN
    var ExternalMotorPort = Port.UNKNOWN

    fun build(byteData: ByteArray): HubNotification {
        return build(convertBytesToString(byteData))
    }

    private fun build(stringData: String): HubNotification {
        when (stringData.substring(6, 8)) {
            "01" -> return ButtonNotification(stringData)
            "82" -> return LedColorChangeNotification(stringData)
            // Sensor data from port
            "45" -> return if (stringData.substring(9, 11) == "01" && ColorSensorPort == Port.C
                    || stringData.substring(9, 11) == "02" && ColorSensorPort == Port.D) {
                ColorSensorNotification(stringData)
            } else if (stringData.substring(9, 11) == "01" && ExternalMotorPort == Port.C
                    || stringData.substring(9, 11) == "02" && ExternalMotorPort == Port.D) {
                ExternalMotorNotification(stringData)
            } else if (stringData.substring(9, 11) == "3A") {
                // TODO: This is tilt sensor data
                UnknownHubNotification(stringData)
            } else {
                InternalMotorNotification(stringData)
            }
            // Port information
            "04" -> return when (stringData.substring(12, 14)) {
                "01" -> PortInfoNotification(stringData)
                "00" -> {
                    when (stringData.substring(9, 11)) {
                        "01" -> {
                            when (Port.C) {
                                ColorSensorPort -> PortDisconnectedNotification(stringData, Sensor.DISTANCE_COLOR)
                                ExternalMotorPort -> PortDisconnectedNotification(stringData, Sensor.EXTERNAL_MOTOR)
                                else -> UnknownHubNotification(stringData)
                            }
                        }
                        "02" -> {
                            when (Port.D) {
                                ColorSensorPort -> PortDisconnectedNotification(stringData, Sensor.DISTANCE_COLOR)
                                ExternalMotorPort -> PortDisconnectedNotification(stringData, Sensor.EXTERNAL_MOTOR)
                                else -> UnknownHubNotification(stringData)
                            }
                        }
                        else -> UnknownHubNotification(stringData)
                    }
                }
                else -> UnknownHubNotification(stringData)
            }
            else -> return UnknownHubNotification(stringData)
        }
    }


}