package com.nateswartz.boostcontroller.enums

private val LED_COLOR_BASE = byteArrayOf(0x08, 0x00, 0x81.toByte(), 0x32, 0x11, 0x51, 0x00)

enum class LEDColorCommand(val data: ByteArray) {
    BLACK(LED_COLOR_BASE + byteArrayOf(Color.BLACK.data)),
    PINK(LED_COLOR_BASE + byteArrayOf(Color.PINK.data)),
    PURPLE(LED_COLOR_BASE + byteArrayOf(Color.PURPLE.data)),
    BLUE(LED_COLOR_BASE + byteArrayOf(Color.BLUE.data)),
    LIGHTBLUE(LED_COLOR_BASE + byteArrayOf(Color.LIGHTBLUE.data)),
    CYAN(LED_COLOR_BASE + byteArrayOf(Color.CYAN.data)),
    GREEN(LED_COLOR_BASE + byteArrayOf(Color.GREEN.data)),
    YELLOW(LED_COLOR_BASE + byteArrayOf(Color.YELLOW.data)),
    ORANGE(LED_COLOR_BASE + byteArrayOf(Color.ORANGE.data)),
    RED(LED_COLOR_BASE + byteArrayOf(Color.RED.data)),
    WHITE(LED_COLOR_BASE + byteArrayOf(Color.WHITE.data)),
}

enum class Color(val data: Byte, val string: String) {
    BLACK(0x00, "Black"),
    PINK(0x01, "Pink"),
    PURPLE(0x02, "Purple"),
    BLUE(0x03, "Blue"),
    LIGHTBLUE(0x04, "Light Blue"),
    CYAN(0x05, "Cyan"),
    GREEN(0x06, "Green"),
    YELLOW(0x07, "Yellow"),
    ORANGE(0x08, "Orange"),
    RED(0x09, "Red"),
    WHITE(0x0A, "White"),
    UNKNOWN(0xFF.toByte(), "Unknown")
}