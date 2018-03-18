package com.nateswartz.boostcontroller

import android.bluetooth.BluetoothGattCharacteristic
import android.content.ContentValues.TAG
import android.util.Log
import java.util.*

val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")!!

class MoveHub (var bluetoothLeService: BluetoothLeService?, val characteristic: BluetoothGattCharacteristic){

    val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
    val ACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    val ACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)

    val ACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    val ACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)

    val C_PORT_BYTE = 0x01.toByte()
    val D_PORT_BYTE = 0x02.toByte()

    var ColorSensorPort = ""
    var ExternalMotorPort = ""

    fun enableNotifications() {
        bluetoothLeService!!.setCharacteristicNotification(characteristic, true)
    }

    fun setLEDColor(color: LEDColor) {
        bluetoothLeService!!.writeCharacteristic(characteristic, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        val powerByte = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        var portByte : Byte? = null
        if (ExternalMotorPort == "C") {
            portByte = C_PORT_BYTE
        } else if (ExternalMotorPort == "D") {
            portByte = D_PORT_BYTE
        }
        val RUN_MOTOR = byteArrayOf(0x0c, 0x00, 0x81.toByte(), portByte!!, 0x11, 0x09, timeBytes[0], timeBytes[1], powerByte, 0x64, 0x7f, 0x03)
        bluetoothLeService!!.writeCharacteristic(characteristic, RUN_MOTOR)
    }

    fun activateButton() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_BUTTON)
    }

    fun activateColorSensor() {
        when (ColorSensorPort) {
            "C" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_COLOR_SENSOR_PORT_C)
            "D" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_COLOR_SENSOR_PORT_D)
        }
    }

    fun activateExternalMotorSensor() {
        when (ExternalMotorPort) {
            "C" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_EXTERNAL_MOTOR_PORT_C)
            "D" -> bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_EXTERNAL_MOTOR_PORT_D)
        }
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
}