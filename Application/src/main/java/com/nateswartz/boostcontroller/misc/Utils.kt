package com.nateswartz.boostcontroller.misc

import com.nateswartz.boostcontroller.enums.Color
import com.nateswartz.boostcontroller.enums.LEDColorCommand

fun getColorFromHex(str: String) : Color {
    return when (str) {
        String.format("%02X", Color.BLACK.data) -> Color.BLACK
        String.format("%02X", Color.PINK.data) -> Color.PINK
        String.format("%02X", Color.PURPLE.data) -> Color.PURPLE
        String.format("%02X", Color.BLUE.data) -> Color.BLUE
        String.format("%02X", Color.LIGHTBLUE.data) -> Color.LIGHTBLUE
        String.format("%02X", Color.CYAN.data) -> Color.CYAN
        String.format("%02X", Color.GREEN.data) -> Color.GREEN
        String.format("%02X", Color.YELLOW.data) -> Color.YELLOW
        String.format("%02X", Color.ORANGE.data) -> Color.ORANGE
        String.format("%02X", Color.RED.data) -> Color.RED
        String.format("%02X", Color.WHITE.data) -> Color.WHITE
        else -> Color.UNKNOWN
    }
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