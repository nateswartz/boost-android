package com.nateswartz.boostcontroller

import android.bluetooth.BluetoothGattCharacteristic
import android.content.ContentValues.TAG
import android.util.Log
import java.util.*

val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")!!

class MoveHub (private var bluetoothLeService: BluetoothLeService?, private var characteristic: BluetoothGattCharacteristic){

    private val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
    private val ACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)

    private val C_PORT_BYTE = 0x01.toByte()
    private val D_PORT_BYTE = 0x02.toByte()
    private val AB_PORT_BYTE = 0x39.toByte()
    private val A_PORT_BYTE = 0x37.toByte()
    private val B_PORT_BYTE = 0x38.toByte()

    private var ColorSensorPort = ""
    private var ExternalMotorPort = ""

    fun update(newBluetoothLeService: BluetoothLeService, newCharacteristic: BluetoothGattCharacteristic) {
        bluetoothLeService = newBluetoothLeService
        characteristic = newCharacteristic
    }

    fun setLEDColor(color: LEDColorCommand) {
        bluetoothLeService!!.writeCharacteristic(characteristic, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        var portByte : Byte? = null
        if (ExternalMotorPort == "C") {
            portByte = C_PORT_BYTE
        } else if (ExternalMotorPort == "D") {
            portByte = D_PORT_BYTE
        }
        runMotor(powerPercentage, timeInMilliseconds, counterclockwise, portByte!!)
    }

    fun runInternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, motor: String)
    {
        when (motor) {
            "A" -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, A_PORT_BYTE)
            "B" -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, B_PORT_BYTE)
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
        bluetoothLeService!!.writeCharacteristic(characteristic, RUN_MOTOR)
    }

    fun enableNotifications() {
        bluetoothLeService!!.setCharacteristicNotification(characteristic, true)
    }

    fun activateButtonNotifications() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_BUTTON)
    }

    fun activateColorSensorNotifications() {
        when (ColorSensorPort) {
            "C" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_COLOR_SENSOR_PORT_C)
            "D" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_COLOR_SENSOR_PORT_D)
        }
    }

    fun activateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            "C" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_EXTERNAL_MOTOR_PORT_C)
            "D" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_EXTERNAL_MOTOR_PORT_D)
        }
    }

    fun activateInternalMotorSensorsNotifications() {
        activateInternalMotorSensorNotifications("A")
        activateInternalMotorSensorNotifications("B")
    }

    fun activateInternalMotorSensorNotifications(motor: String) {
        var data = ACTIVATE_MOTOR_PORT
        when (motor) {
            "A" -> data[3] = A_PORT_BYTE
            "B" -> data[3] = B_PORT_BYTE
        }
        bluetoothLeService!!.writeCharacteristic(characteristic, data)
    }

    fun activateTiltSensorNotifications() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_TILT_SENSOR)
    }

    fun handleNotification(data: String) {
        val pieces = data.split("\n")
        val encodedData = pieces[pieces.size - 1]
        val notification = HubNotificationFactory.build(encodedData.trim())
        if (notification is PortInfoNotification) {
            when(notification.sensor) {
                "DistanceColor" -> {
                    ColorSensorPort = notification.port
                    HubNotificationFactory.ColorSensorPort = notification.port
                }
                "ExternalMotor" -> {
                    ExternalMotorPort = notification.port
                    HubNotificationFactory.ExternalMotorPort = notification.port
                }
            }
        }
        Log.e(TAG, notification.toString())
    }

    private fun runMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, portByte: Byte) {
        val powerByte = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val RUN_MOTOR = byteArrayOf(0x0c, 0x00, 0x81.toByte(), portByte, 0x11, 0x09, timeBytes[0], timeBytes[1], powerByte, 0x64, 0x7f, 0x03)
        bluetoothLeService!!.writeCharacteristic(characteristic, RUN_MOTOR)
    }
}