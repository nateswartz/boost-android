package com.nateswartz.boostcontroller

import android.bluetooth.BluetoothGattCharacteristic
import android.content.ContentValues.TAG
import android.util.Log
import java.util.*

val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")!!

class MoveHub (var bluetoothLeService: BluetoothLeService?, val characteristic: BluetoothGattCharacteristic){

    val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
    val ACTIVATE_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    val ACTIVATE_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)

    fun enableNotifications() {
        bluetoothLeService!!.setCharacteristicNotification(characteristic, true)
    }

    fun setLEDColor(color: LEDColor) {
        bluetoothLeService!!.writeCharacteristic(characteristic, color.data)
    }

    fun activateButton() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_BUTTON)
    }

    fun activateColorSensor() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_PORT_C)
    }

    fun parseNotification(notification: String): String {
        var parsedNotification = "Unknown notification"
        when (notification) {
            "05 00 82 32 0A" -> parsedNotification = "LED Color Changed"
            "06 00 01 02 06 00" -> parsedNotification = "Button released"
            "06 00 01 02 06 01" -> parsedNotification = "Button pressed"
        }
        if (notification.startsWith("08 00 45")) {
            parsedNotification = "Sensor data - "

            if ("${notification[10]}" == "1") {
                parsedNotification += "Port C - "
            } else {
                parsedNotification += "Port D - "
            }

            when ("${notification[12]}${notification[13]}") {
                String.format("%02X", Color.BLACK.data) -> parsedNotification += "Black - "
                String.format("%02X", Color.BLUE.data) -> parsedNotification += "Blue - "
                String.format("%02X", Color.GREEN.data) -> parsedNotification += "Green - "
                String.format("%02X", Color.YELLOW.data) -> parsedNotification += "Yellow - "
                String.format("%02X", Color.RED.data) -> parsedNotification += "Red - "
                String.format("%02X", Color.WHITE.data) -> parsedNotification += "White - "
                else -> parsedNotification += "No Color - "
            }

            parsedNotification += "${notification[15]}${notification[16]} inches"
          //0f 00 04 01 01 25 00 00 00 00 10 00 00 00 10
        } else if (notification.startsWith("0f 00 04")) {
            parsedNotification += "Port info - "
            when ("${notification[9]}${notification[10]}") {
                "01" -> parsedNotification += "Port C - "
                "02" -> parsedNotification += "Port D - "
                "37" -> parsedNotification += "Port A - "
                "38" -> parsedNotification += "Port B - "
            }
        }
        return parsedNotification
    }
}
private val LED_COLOR_BASE = byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00)

enum class LEDColor(val data: ByteArray) {

    WHITE(LED_COLOR_BASE + byteArrayOf(Color.WHITE.data)),
    YELLOW(LED_COLOR_BASE + byteArrayOf(Color.YELLOW.data)),
    PINK(LED_COLOR_BASE + byteArrayOf(Color.PINK.data)),
    PURPLE(LED_COLOR_BASE + byteArrayOf(Color.PURPLE.data)),
}

enum class Color(val data: Byte, val string: String) {
    WHITE(0x0A, "White"),
    YELLOW(0x07, "Yellow"),
    PINK(0x01, "Pink"),
    PURPLE(0x02, "Purple"),
    BLACK(0x00, "Black"),
    BLUE(0x03, "Blue"),
    GREEN(0x05, "Green"),
    RED(0x09, "Red")
}