package com.nateswartz.boostcontroller.misc

import com.nateswartz.boostcontroller.enums.Color
import com.nateswartz.boostcontroller.enums.LEDColorCommand

fun getColorFromHex(hex: Byte) : Color {
    enumValues<Color>().forEach {
        if (it.data == hex) {
            return it
        }
    }
    return Color.UNKNOWN
}

fun getLedColorFromName(name: String) : LEDColorCommand {
    return (LEDColorCommand.valueOf(name.toUpperCase()))
}

fun getByteArrayFromInt(number: Int, size: Int): ByteArray {
    val result = ByteArray(size)
    var intermediateNumber = number
    val mask = 0xFF

    for ( i in 0 until result.size) {
        result[i] = intermediateNumber.and(mask).toByte()
        intermediateNumber = intermediateNumber.shr(8)
    }
    return result
}

fun convertBytesToString(bytes: ByteArray): String {
    val stringBuilder = StringBuilder(bytes.size)
    for (byteChar in bytes)
        stringBuilder.append(String.format("%02X ", byteChar))
    val pieces = (String(bytes) + "\n" + stringBuilder.toString()).split("\n")
    return pieces[pieces.size - 1].trim()
}