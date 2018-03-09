package com.nateswartz.boostcontroller

import android.bluetooth.BluetoothGattCharacteristic
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.util.*

/**
 * Created by nates on 3/8/2018.
 */
val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")

class MoveHub (var bluetoothLeService: BluetoothLeService?, val characteristic: BluetoothGattCharacteristic){

    val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)

    fun setLEDColor(color: LEDColor) {
        bluetoothLeService!!.writeCharacteristic(characteristic, color.data)
    }

    fun activateButton() {
        bluetoothLeService!!.writeCharacteristic(characteristic, ACTIVATE_BUTTON)
    }
}

enum class LEDColor(val data: ByteArray) {
    WHITE(byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00, 0x0A)),
    YELLOW(byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00, 0x07)),
    PINK(byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00, 0x01)),
    PURPLE(byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00, 0x02)),
}