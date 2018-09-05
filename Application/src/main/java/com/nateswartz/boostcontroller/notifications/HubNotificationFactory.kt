package com.nateswartz.boostcontroller.notifications

import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.BoostSensor
import com.nateswartz.boostcontroller.misc.convertBytesToString

object HubNotificationFactory {

    var ColorSensorPort = BoostPort.NONE
    var ExternalMotorPort = BoostPort.NONE

    fun build(byteData: ByteArray): HubNotification {
        return when (byteData[2]) {
            0x01.toByte() -> ButtonNotification(byteData)
            0x82.toByte() -> when (byteData[3]) {
                0x32.toByte() -> LedColorChangeNotification(byteData)
                else -> MotorMovementChangeNotification(byteData)
            }
            0x45.toByte() -> when(byteData[3]) {
                ColorSensorPort.code -> ColorSensorNotification(convertBytesToString(byteData))
                ExternalMotorPort.code -> ExternalMotorNotification(byteData)
                0x3A.toByte() -> when (byteData[1]) {
                    0x05.toByte() -> TiltSensorNotification((byteData))
                    else -> AdvancedTiltSensorNotification((byteData))
                }
                else -> InternalMotorNotification(convertBytesToString(byteData))
            }
            0x04.toByte() -> when (byteData[4]) {
                0x01.toByte() -> PortInfoNotification(byteData)
                0x00.toByte() -> when (byteData[3]) {
                    ColorSensorPort.code -> PortDisconnectedNotification(byteData, BoostSensor.DISTANCE_COLOR)
                    ExternalMotorPort.code -> PortDisconnectedNotification(byteData, BoostSensor.EXTERNAL_MOTOR)
                    else -> UnknownHubNotification(byteData)
                }
                else -> UnknownHubNotification(byteData)
            }
            else -> UnknownHubNotification(byteData)
        }
    }



}