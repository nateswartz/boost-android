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
        val portByte = when (externalMotorPort) {
            Port.C -> C_PORT_BYTE
            Port.D -> D_PORT_BYTE
            else -> {
                Log.w(TAG, "No external motor detected")
                return
            }
        }
        runMotor(powerPercentage, timeInMilliseconds, counterclockwise, portByte)
    }

    fun runInternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, motor: Port)
    {
        when (motor) {
            Port.A -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, A_PORT_BYTE)
            Port.B -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, B_PORT_BYTE)
            else -> Log.w(TAG, "Invalid motor specified")
        }
    }

    fun runInternalMotors(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(powerPercentage, timeInMilliseconds, counterclockwise, AB_PORT_BYTE)
    }

    fun runInternalMotorsInOpposition(powerPercentage: Int, timeInMilliseconds: Int) {
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val motorAPower = powerPercentage.toByte()
        val motorBPower = (255 - powerPercentage).toByte()
        val runMotorCommand = byteArrayOf(0x0d, 0x00, 0x81.toByte(), AB_PORT_BYTE, 0x11, 0x0a, timeBytes[0], timeBytes[1], motorAPower, motorBPower, 0x64, 0x7f, 0x03)
        gattController.writeCharacteristic(DeviceType.BOOST, runMotorCommand)
    }

    fun enableNotifications() {
        gattController.setCharacteristicNotification(DeviceType.BOOST, true)
    }

    fun activateButtonNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_BUTTON)
    }

    // Currently not working
    fun deactivateButtonNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_BUTTON)
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
        val message = MoveHubMessageFactory.getExternalSensorMessage(port, type, enable)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun activateInternalMotorSensorsNotifications() {
        activateInternalMotorSensorNotifications(InternalMotorNotificationPort.A, MotorNotificationType.ANGLE)
        activateInternalMotorSensorNotifications(InternalMotorNotificationPort.B, MotorNotificationType.ANGLE)
    }

    private fun activateInternalMotorSensorNotifications(motor: InternalMotorNotificationPort, type: MotorNotificationType) {
        val message = MoveHubMessageFactory.getInternalMotorMessage(motor, type, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateInternalMotorSensorsNotifications() {
        deactivateInternalMotorSensorNotifications(InternalMotorNotificationPort.A)
        deactivateInternalMotorSensorNotifications(InternalMotorNotificationPort.B)
    }

    private fun deactivateInternalMotorSensorNotifications(motor: InternalMotorNotificationPort) {
        val message = MoveHubMessageFactory.getInternalMotorMessage(motor, enable = false)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun activateTiltSensorNotifications() {
        val message = MoveHubMessageFactory.getTiltSensorMessage(false, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateTiltSensorNotifications() {
        val data = MoveHubMessageFactory.getTiltSensorMessage(false, false)
        gattController.writeCharacteristic(DeviceType.BOOST, data)
    }

    fun activateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.getTiltSensorMessage(true, true)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.getTiltSensorMessage(true, false)
        gattController.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun runMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, portByte: Byte) {
        val powerByte = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val runMotorCommand = byteArrayOf(0x0c, 0x00, 0x81.toByte(), portByte, 0x11, 0x09, timeBytes[0], timeBytes[1], powerByte, 0x64, 0x7f, 0x03)
        gattController.writeCharacteristic(DeviceType.BOOST, runMotorCommand)
    }

    companion object {
        private val TAG = MoveHubController::class.java.simpleName

        private val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
        // Currently not working
        private val DEACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x03, 0x02, 0x00)

        private const val C_PORT_BYTE = 0x01.toByte()
        private const val D_PORT_BYTE = 0x02.toByte()
        private const val AB_PORT_BYTE = 0x39.toByte()
        private const val A_PORT_BYTE = 0x37.toByte()
        private const val B_PORT_BYTE = 0x38.toByte()
    }
}

enum class MotorNotificationType {
    ANGLE, SPEED, NONE
}

enum class InternalMotorNotificationPort {
    A, B, A_B, NONE
}

enum class MotorPort {
    A, B, C, D, NONE
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

    fun getInternalMotorMessage(port: InternalMotorNotificationPort = InternalMotorNotificationPort.NONE,
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

    fun getTiltSensorMessage(advanced: Boolean = false,
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

    fun getExternalSensorMessage(port: ExternalSensorPort = ExternalSensorPort.NONE,
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

    // Only for single motor for now
    fun getRunMotorMessage(port: MotorPort = MotorPort.NONE,
                           timeInMS: Int,
                           powerPercentage: Int,
                           counterclockwise: Boolean): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SET_PORT_VALUE

        message += when (port) {
            MotorPort.A -> PORT_A
            MotorPort.B -> PORT_B
            MotorPort.C -> PORT_C
            MotorPort.D -> PORT_D
            MotorPort.NONE -> EMPTY
        }

        message += byteArrayOf(0x11, 0x09)

        message += getByteArrayFromInt(timeInMS, 2)

        message += when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }

        message += byteArrayOf(0x64, 0x7f, 0x03)

        message = (message.size + 1).toString(16).toByteArray() + message

        return message
    }
}