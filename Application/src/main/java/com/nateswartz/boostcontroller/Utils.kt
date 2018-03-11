package com.nateswartz.boostcontroller

fun getColor(str: String) : Color {
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