package com.nateswartz.boostcontroller.controllers

import android.util.Log
import com.nateswartz.boostcontroller.enums.LEDColorCommand
import com.nateswartz.boostcontroller.misc.getByteArrayFromInt
import com.nateswartz.boostcontroller.notifications.Port

class MoveHubController (private val gattController: GattController) {

    var ColorSensorPort = Port.UNKNOWN
    var ExternalMotorPort = Port.UNKNOWN

    fun setLEDColor(color: LEDColorCommand) {
        gattController.writeCharacteristic(DeviceType.BOOST, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        val portByte = when (ExternalMotorPort) {
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
        val RUN_MOTOR = byteArrayOf(0x0d, 0x00, 0x81.toByte(), AB_PORT_BYTE, 0x11, 0x0a, timeBytes[0], timeBytes[1], motorAPower, motorBPower, 0x64, 0x7f, 0x03)
        gattController.writeCharacteristic(DeviceType.BOOST, RUN_MOTOR)
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
        when (ColorSensorPort) {
            Port.C -> gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_COLOR_SENSOR_PORT_C)
            Port.D -> gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_COLOR_SENSOR_PORT_D)
            else -> Log.w(TAG, "No Color Sensor detected")
        }
    }

    fun deactivateColorSensorNotifications() {
        when (ColorSensorPort) {
            Port.C -> gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_COLOR_SENSOR_PORT_C)
            Port.D -> gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_COLOR_SENSOR_PORT_D)
            else -> Log.w(TAG, "No Color Sensor detected")
        }
    }

    fun activateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            Port.C -> gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_EXTERNAL_MOTOR_PORT_C)
            Port.D -> gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_EXTERNAL_MOTOR_PORT_D)
            else -> Log.w(TAG, "No External Motor detected")
        }
    }

    fun deactivateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            Port.C -> gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_EXTERNAL_MOTOR_PORT_C)
            Port.D -> gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_EXTERNAL_MOTOR_PORT_D)
            else -> Log.w(TAG, "No External Motor detected")
        }
    }

    fun activateInternalMotorSensorsNotifications() {
        activateInternalMotorSensorNotifications("A", "angle")
        activateInternalMotorSensorNotifications("B", "angle")
        //activateInternalMotorSensorNotifications("A+B")
    }

    fun activateInternalMotorSensorNotifications(motor: String, type: String) {
        val data = ACTIVATE_MOTOR_PORT
        when (motor) {
            "A" -> data[3] = A_PORT_BYTE
            "B" -> data[3] = B_PORT_BYTE
            "A+B" -> data[3] = AB_PORT_BYTE
        }
        when (type) {
            "speed" -> data[4] = SPEED_BYTE
            "angle" -> data[4] = ANGLE_BYTE
        }
        gattController.writeCharacteristic(DeviceType.BOOST, data)
    }

    fun deactivateInternalMotorSensorsNotifications() {
        deactivateInternalMotorSensorNotifications("A")
        deactivateInternalMotorSensorNotifications("B")
    }

    fun deactivateInternalMotorSensorNotifications(motor: String) {
        val data = DEACTIVATE_MOTOR_PORT
        when (motor) {
            "A" -> data[3] = A_PORT_BYTE
            "B" -> data[3] = B_PORT_BYTE
        }
        gattController.writeCharacteristic(DeviceType.BOOST, data)
    }

    fun activateTiltSensorNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, ACTIVATE_TILT_SENSOR)
    }

    fun deactivateTiltSensorNotifications() {
        gattController.writeCharacteristic(DeviceType.BOOST, DEACTIVATE_TILT_SENSOR)
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

        private val ACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val ACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val DEACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00)
        private val DEACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00)

        private val ACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val ACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val DEACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)
        private val DEACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)

        private val ACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val DEACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00)

        private val ACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
        private val DEACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)

        private val C_PORT_BYTE = 0x01.toByte()
        private val D_PORT_BYTE = 0x02.toByte()
        private val AB_PORT_BYTE = 0x39.toByte()
        private val A_PORT_BYTE = 0x37.toByte()
        private val B_PORT_BYTE = 0x38.toByte()

        private val SPEED_BYTE = 0x01.toByte()
        private val ANGLE_BYTE = 0x02.toByte()
    }
}
