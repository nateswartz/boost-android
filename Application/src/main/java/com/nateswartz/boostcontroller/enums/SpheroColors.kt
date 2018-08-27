package com.nateswartz.boostcontroller.enums

enum class SpheroColors(val data: Triple<Float, Float, Float>) {
    RED(Triple(1.0f, 0.0f, 0.0f)),
    GREEN(Triple(0.0f, 1.0f, 0.0f)),
    BLUE(Triple(0.0f, 0.0f, 1.0f)),
    YELLOW(Triple(1.0f, 1.0f, 0.0f)),
    PURPLE(Triple(1.0f, 0.0f, 1.0f)),
    WHITE(Triple(1.0f, 1.0f, 1.0f)),
    BLACK(Triple(0.0f, 0.0f, 0.0f))
}