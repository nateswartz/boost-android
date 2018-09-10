package com.nateswartz.boostcontroller.notifications

import com.nateswartz.boostcontroller.enums.BoostPort
import com.nateswartz.boostcontroller.enums.BoostSensor
import com.nateswartz.boostcontroller.misc.convertBytesToString

object HubNotificationFactory {

    var ColorSensorPort = BoostPort.NONE
    var ExternalMotorPort = BoostPort.NONE

    private const val DEVICE_INFORMATION = 0x01.toByte()
    private const val PONG = 0x03.toByte()
    private const val PORT_CHANGED = 0x82.toByte()
    private const val SENSOR_READING = 0x45.toByte()
    private const val PORT_INFORMATION = 0x04.toByte()

    private const val NAME = 0x01.toByte()
    private const val BUTTON = 0x02.toByte()
    private const val LED = 0x32.toByte()
    private const val TILT_SENSOR = 0x3A.toByte()

    private const val DEVICE = 0x01.toByte()
    private const val NO_DEVICE = 0x00.toByte()

    fun build(byteData: ByteArray): HubNotification {
        val length = byteData[0]
        val messageType = byteData[2]
        val portNumber = byteData[3]
        val deviceKind = byteData[4]

        return when (messageType) {
            DEVICE_INFORMATION -> when (portNumber) {
                NAME -> NameNotification(byteData)
                BUTTON -> ButtonNotification(byteData)
                else -> UnknownHubNotification(byteData)
            }
            PORT_CHANGED -> when (portNumber) {
                LED -> LedColorChangeNotification(byteData)
                else -> MotorMovementChangeNotification(byteData)
            }
            SENSOR_READING -> when(portNumber) {
                ColorSensorPort.code -> ColorSensorNotification(byteData)
                ExternalMotorPort.code -> ExternalMotorNotification(byteData)
                TILT_SENSOR -> when (length) {
                    0x05.toByte() -> TiltSensorNotification((byteData))
                    else -> AdvancedTiltSensorNotification((byteData))
                }
                else -> InternalMotorNotification(convertBytesToString(byteData))
            }
            PORT_INFORMATION -> when (deviceKind) {
                DEVICE -> PortConnectedNotification(byteData)
                NO_DEVICE -> when (portNumber) {
                    ColorSensorPort.code -> PortDisconnectedNotification(byteData, BoostSensor.DISTANCE_COLOR)
                    ExternalMotorPort.code -> PortDisconnectedNotification(byteData, BoostSensor.EXTERNAL_MOTOR)
                    else -> UnknownHubNotification(byteData)
                }
                else -> UnknownHubNotification(byteData)
            }
            PONG -> PongNotification(byteData)
            else -> UnknownHubNotification(byteData)
        }
    }
}