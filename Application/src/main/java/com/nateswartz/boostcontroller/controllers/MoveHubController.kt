package com.nateswartz.boostcontroller.controllers

import android.util.Log
import com.nateswartz.boostcontroller.enums.LEDColorCommand
import com.nateswartz.boostcontroller.misc.getByteArrayFromInt
import com.nateswartz.boostcontroller.notifications.Port

class MoveHubController (private val gattController: GattController) {

    var colorSensorPort = Port.UNKNOWN
    var externalMotorPort = Port.UNKNOWN

    fun setLEDColor(color: LEDColorCommand) {
        gattController.writeCharacteristic(DeviceType.BOOST, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(externalMotorPort, timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, motor: Port) {
        runMotor(motor, timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotors(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(Port.A_B,timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotorsInOpposition(powerPercentage: Int, timeInMilliseconds: Int) {
        val message = MoveHubMessageFactory.runMotor(MotorPort.A_B, timeInMilliseconds, powerPercentage, false, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun enableNotifications() {
        gattController.setCharacteristicNotification(DeviceType.BOOST, true)
    }

    fun activateButtonNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02))
    }

    // Currently not working
    fun deactivateButtonNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, byteArrayOf(0x05, 0x00, 0x03, 0x02, 0x00))
    }

    fun activateColorSensorNotifications() {
        changeColorSensorNotifications(true)
    }

    fun deactivateColorSensorNotifications() {
        changeColorSensorNotifications(false)
    }

    fun activateExternalMotorSensorNotifications() {
        changeExternalMotorSensorNotifications(true)
    }

    fun deactivateExternalMotorSensorNotifications() {
        changeExternalMotorSensorNotifications(false)
    }

    fun activateInternalMotorSensorsNotifications() {
        activateInternalMotorSensorNotifications(InternalMotorNotificationPort.A, MotorNotificationType.ANGLE)
        activateInternalMotorSensorNotifications(InternalMotorNotificationPort.B, MotorNotificationType.ANGLE)
    }

    fun deactivateInternalMotorSensorsNotifications() {
        deactivateInternalMotorSensorNotifications(InternalMotorNotificationPort.A)
        deactivateInternalMotorSensorNotifications(InternalMotorNotificationPort.B)
    }

    fun activateTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(false, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateTiltSensorNotifications() {
        val data = MoveHubMessageFactory.tiltSensorNotifications(false, false)
        gattController.writeCharacteristic(DeviceType.BOOST, data)
    }

    fun activateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(true, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(true, false)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun activateInternalMotorSensorNotifications(motor: InternalMotorNotificationPort, type: MotorNotificationType) {
        val message = MoveHubMessageFactory.internalMotorNotifications(motor, type, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun deactivateInternalMotorSensorNotifications(motor: InternalMotorNotificationPort) {
        val message = MoveHubMessageFactory.internalMotorNotifications(motor, enable = false)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun changeColorSensorNotifications(enable: Boolean) {
        changeExternalSensorNotifications(enable, colorSensorPort, ExternalSensorType.COLOR)
    }

    private fun changeExternalMotorSensorNotifications(enable: Boolean) {
        changeExternalSensorNotifications(enable, externalMotorPort, ExternalSensorType.MOTOR)
    }

    private fun changeExternalSensorNotifications(enable: Boolean, portToRead: Port, type: ExternalSensorType) {
        val port = when (portToRead) {
            Port.C -> ExternalSensorPort.C
            Port.D -> ExternalSensorPort.D
            else -> {
                Log.w(TAG, "No External Sensor detected")
                ExternalSensorPort.NONE
            }
        }
        val message = MoveHubMessageFactory.externalSensorNotificaions(port, type, enable)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun runMotor(motor: Port, timeInMilliseconds: Int, powerPercentage: Int, counterclockwise: Boolean) {
        val port = when (motor) {
            Port.A -> MotorPort.A
            Port.B -> MotorPort.B
            Port.A_B -> MotorPort.A_B
            Port.C -> MotorPort.C
            Port.D -> MotorPort.D
            Port.UNKNOWN -> {
                Log.w(TAG, "No Motor Specified")
                MotorPort.NONE
            }
        }
        val message = MoveHubMessageFactory.runMotor(port, timeInMilliseconds, powerPercentage, counterclockwise)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    companion object {
        private val TAG = MoveHubController::class.java.simpleName
    }
}

enum class MotorNotificationType {
    ANGLE, SPEED, NONE
}

enum class InternalMotorNotificationPort {
    A, B, A_B, NONE
}

enum class MotorPort {
    A, B, A_B, C, D, NONE
}


enum class ExternalSensorPort {
    C, D, NONE
}

enum class ExternalSensorType {
    COLOR, MOTOR, NONE
}

object MoveHubMessageFactory {
    private const val PROTOCOL_VERSION = 0x00.toByte()

    private const val SUBSCRIBE_TO_SENSOR = 0x41.toByte()
    private const val SET_PORT_VALUE = 0x81.toByte()

    private const val TILT_SENSOR_PORT = 0x3a.toByte()

    private const val ENABLE_NOTIFICATION = 0x01.toByte()
    private const val DISABLE_NOTIFICATION = 0x00.toByte()

    private const val EMPTY = 0x00.toByte()

    private const val PORT_A = 0x37.toByte()
    private const val PORT_B = 0x38.toByte()
    private const val PORT_A_B = 0x39.toByte()
    private const val PORT_C = 0x01.toByte()
    private const val PORT_D = 0x02.toByte()

    private const val MOTOR_SPEED = 0x01.toByte()
    private const val MOTOR_ANGLE = 0x02.toByte()

    fun internalMotorNotifications(port: InternalMotorNotificationPort = InternalMotorNotificationPort.NONE,
                                type: MotorNotificationType = MotorNotificationType.NONE,
                                enable: Boolean = true): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SUBSCRIBE_TO_SENSOR

        message += when (port) {
            InternalMotorNotificationPort.A -> PORT_A
            InternalMotorNotificationPort.B -> PORT_B
            InternalMotorNotificationPort.A_B -> PORT_A_B
            InternalMotorNotificationPort.NONE -> EMPTY
        }

        message += when (type) {
            MotorNotificationType.SPEED -> MOTOR_SPEED
            MotorNotificationType.ANGLE -> MOTOR_ANGLE
            MotorNotificationType.NONE -> EMPTY
        }
        message += byteArrayOf(0x01, 0x00, 0x00, 0x00)

        message += when (enable) {
            true -> ENABLE_NOTIFICATION
            false -> DISABLE_NOTIFICATION
        }

        message = (message.size + 1).toString(16).toByteArray() + message

        return message
    }

    fun tiltSensorNotifications(advanced: Boolean = false,
                             enable: Boolean = true): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SUBSCRIBE_TO_SENSOR
        message += TILT_SENSOR_PORT

        message += when (advanced) {
            true -> byteArrayOf(0x04, 0x10)
            false -> byteArrayOf(0x02, 0x01)
        }

        message += byteArrayOf(0x00, 0x00, 0x00)

        message += when (enable) {
            true -> ENABLE_NOTIFICATION
            false -> DISABLE_NOTIFICATION
        }

        message = (message.size + 1).toString(16).toByteArray() + message

        return message
    }

    fun externalSensorNotificaions(port: ExternalSensorPort = ExternalSensorPort.NONE,
                                 type: ExternalSensorType = ExternalSensorType.NONE,
                                 enable: Boolean = true): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SUBSCRIBE_TO_SENSOR

        message += when (port) {
            ExternalSensorPort.C -> 0x01.toByte()
            ExternalSensorPort.D -> 0x02.toByte()
            ExternalSensorPort.NONE -> EMPTY
        }

        message += when (type) {
            ExternalSensorType.COLOR -> 0x08.toByte()
            ExternalSensorType.MOTOR -> 0x02.toByte()
            ExternalSensorType.NONE -> EMPTY
        }

        message += byteArrayOf(0x01, 0x00, 0x00, 0x00)

        message += when (enable) {
            true -> ENABLE_NOTIFICATION
            false -> DISABLE_NOTIFICATION
        }

        message = (message.size + 1).toString(16).toByteArray() + message

        return message
    }

    fun runMotor(port: MotorPort = MotorPort.NONE,
                           timeInMS: Int,
                           powerPercentage: Int,
                           counterclockwise: Boolean,
                           inOpposition: Boolean = false): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SET_PORT_VALUE

        message += when (port) {
            MotorPort.A -> PORT_A
            MotorPort.B -> PORT_B
            MotorPort.A_B -> PORT_A_B
            MotorPort.C -> PORT_C
            MotorPort.D -> PORT_D
            MotorPort.NONE -> EMPTY
        }

        message += 0x11.toByte()

        message += when (inOpposition) {
            true -> 0x0a.toByte()
            false -> 0x09.toByte()
        }

        val time = getByteArrayFromInt(timeInMS, 2)
        message += time

        val power = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        message += power

        if (inOpposition) {
            message += when (counterclockwise) {
                true -> powerPercentage.toByte()
                false -> (255 - powerPercentage).toByte()
            }
        }

        message += byteArrayOf(0x64, 0x7f, 0x03)

        message = (message.size + 1).toString(16).toByteArray() + message

        return message
    }
}