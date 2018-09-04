package com.nateswartz.boostcontroller.notifications

import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.BoostSensor
import com.nateswartz.boostcontroller.misc.convertBytesToString

object HubNotificationFactory {

    var ColorSensorPort = BoostPort.NONE
    var ExternalMotorPort = BoostPort.NONE

    fun build(byteData: ByteArray): HubNotification {
        return build(convertBytesToString(byteData))
    }

    private fun build(stringData: String): HubNotification {
        when (stringData.substring(6, 8)) {
            "01" -> return ButtonNotification(stringData)
            "82" -> return LedColorChangeNotification(stringData)
            // Sensor data from port
            "45" -> return if (stringData.substring(9, 11) == "01" && ColorSensorPort == BoostPort.C
                    || stringData.substring(9, 11) == "02" && ColorSensorPort == BoostPort.D) {
                ColorSensorNotification(stringData)
            } else if (stringData.substring(9, 11) == "01" && ExternalMotorPort == BoostPort.C
                    || stringData.substring(9, 11) == "02" && ExternalMotorPort == BoostPort.D) {
                ExternalMotorNotification(stringData)
            } else if (stringData.substring(9, 11) == "3A") {
                if (stringData.substring(0, 2) > "05") {
                    AdvancedTiltSensorNotification(stringData)
                } else {
                    TiltSensorNotification(stringData)
                }
            } else {
                InternalMotorNotification(stringData)
            }
            // Port information
            "04" -> return when (stringData.substring(12, 14)) {
                "01" -> PortInfoNotification(stringData)
                "00" -> {
                    when (stringData.substring(9, 11)) {
                        "01" -> {
                            when (BoostPort.C) {
                                ColorSensorPort -> PortDisconnectedNotification(stringData, BoostSensor.DISTANCE_COLOR)
                                ExternalMotorPort -> PortDisconnectedNotification(stringData, BoostSensor.EXTERNAL_MOTOR)
                                else -> UnknownHubNotification(stringData)
                            }
                        }
                        "02" -> {
                            when (BoostPort.D) {
                                ColorSensorPort -> PortDisconnectedNotification(stringData, BoostSensor.DISTANCE_COLOR)
                                ExternalMotorPort -> PortDisconnectedNotification(stringData, BoostSensor.EXTERNAL_MOTOR)
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