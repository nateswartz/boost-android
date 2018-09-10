package com.nateswartz.boostcontroller.controllers

import android.util.Log
import com.nateswartz.boostcontroller.enums.*
import com.nateswartz.boostcontroller.misc.getByteArrayFromInt

class MoveHubController (private val gattWriter: GattWriter) {

    var colorSensorPort = BoostPort.NONE
    var externalMotorPort = BoostPort.NONE

    fun setLEDColor(color: LEDColorCommand) {
        gattWriter.writeCharacteristic(DeviceType.BOOST, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(externalMotorPort, timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, motor: BoostPort) {
        runMotor(motor, timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotors(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(BoostPort.A_B,timeInMilliseconds, powerPercentage, counterclockwise)
    }

    fun runInternalMotorsInOpposition(powerPercentage: Int, timeInMilliseconds: Int) {
        val message = MoveHubMessageFactory.runMotor(BoostPort.A_B, timeInMilliseconds, powerPercentage, false, true)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun enableNotifications() {
        gattWriter.setCharacteristicNotification(DeviceType.BOOST, true)
    }

    fun activateButtonNotifications() {
        gattWriter.writeCharacteristic(DeviceType.BOOST, byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02))
    }

    fun deactivateButtonNotifications() {
        gattWriter.writeCharacteristic(DeviceType.BOOST, byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x03))
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
        activateInternalMotorSensorNotifications(InternalMotorPort.A, MotorNotificationType.ANGLE)
        activateInternalMotorSensorNotifications(InternalMotorPort.B, MotorNotificationType.ANGLE)
    }

    fun deactivateInternalMotorSensorsNotifications() {
        deactivateInternalMotorSensorNotifications(InternalMotorPort.A)
        deactivateInternalMotorSensorNotifications(InternalMotorPort.B)
    }

    fun activateTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(false, true)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateTiltSensorNotifications() {
        val data = MoveHubMessageFactory.tiltSensorNotifications(false, false)
        gattWriter.writeCharacteristic(DeviceType.BOOST, data)
    }

    fun activateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(true, true)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun deactivateAdvancedTiltSensorNotifications() {
        val message = MoveHubMessageFactory.tiltSensorNotifications(true, false)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun getName() {
        val message = byteArrayOf(0x06, 0x00, 0x01, 0x01, 0x02, 0x00)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    fun ping() {
        val message = byteArrayOf(0x06, 0x00, 0x03, 0x01, 0x03, 0x00)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun activateInternalMotorSensorNotifications(motor: InternalMotorPort, type: MotorNotificationType) {
        val message = MoveHubMessageFactory.internalMotorNotifications(motor, type, true)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun deactivateInternalMotorSensorNotifications(motor: InternalMotorPort) {
        val message = MoveHubMessageFactory.internalMotorNotifications(motor, enable = false)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun changeColorSensorNotifications(enable: Boolean) {
        changeExternalSensorNotifications(enable, colorSensorPort, ExternalSensorType.COLOR)
    }

    private fun changeExternalMotorSensorNotifications(enable: Boolean) {
        changeExternalSensorNotifications(enable, externalMotorPort, ExternalSensorType.MOTOR)
    }

    private fun changeExternalSensorNotifications(enable: Boolean, portToRead: BoostPort, type: ExternalSensorType) {
        val port = when (portToRead) {
            BoostPort.C -> ExternalSensorPort.C
            BoostPort.D -> ExternalSensorPort.D
            else -> {
                Log.w(TAG, "No External Sensor detected")
                ExternalSensorPort.NONE
            }
        }
        val message = MoveHubMessageFactory.externalSensorNotifications(port, type, enable)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    private fun runMotor(motor: BoostPort, timeInMilliseconds: Int, powerPercentage: Int, counterclockwise: Boolean) {
        val message = MoveHubMessageFactory.runMotor(motor, timeInMilliseconds, powerPercentage, counterclockwise)
        gattWriter.writeCharacteristic(DeviceType.BOOST, message)
    }

    companion object {
        private val TAG = MoveHubController::class.java.simpleName
    }
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

    fun internalMotorNotifications(port: InternalMotorPort = InternalMotorPort.NONE,
                                type: MotorNotificationType = MotorNotificationType.NONE,
                                enable: Boolean = true): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SUBSCRIBE_TO_SENSOR

        message += when (port) {
            InternalMotorPort.A -> PORT_A
            InternalMotorPort.B -> PORT_B
            InternalMotorPort.A_B -> PORT_A_B
            InternalMotorPort.NONE -> EMPTY
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

    fun externalSensorNotifications(port: ExternalSensorPort = ExternalSensorPort.NONE,
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

    fun runMotor(port: BoostPort = BoostPort.NONE,
                           timeInMS: Int,
                           powerPercentage: Int,
                           counterclockwise: Boolean,
                           inOpposition: Boolean = false): ByteArray {

        var message = byteArrayOf(PROTOCOL_VERSION)
        message += SET_PORT_VALUE

        message += when (port) {
            BoostPort.A -> PORT_A
            BoostPort.B -> PORT_B
            BoostPort.A_B -> PORT_A_B
            BoostPort.C -> PORT_C
            BoostPort.D -> PORT_D
            else -> EMPTY
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