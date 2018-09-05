package com.nateswartz.boostcontroller.enums

enum class MotorNotificationType {
    ANGLE, SPEED, NONE
}

enum class InternalMotorPort {
    A, B, A_B, NONE
}

enum class BoostPort(val code: Byte) {
    A(0x37),
    B(0x38),
    A_B(0x39),
    C(0x01),
    D(0x02),
    NONE(0x00)
}

fun findBoostPort(code: Byte) : BoostPort {
    enumValues<BoostPort>().forEach {
        if (it.code == code) {
            return it
        }
    }
    return BoostPort.NONE
}

enum class ExternalSensorPort {
    C, D, NONE
}

enum class ExternalSensorType {
    COLOR, MOTOR, NONE
}

enum class BoostSensor(val code: Byte) {
    LED(0x17),
    DISTANCE_COLOR(0x25),
    EXTERNAL_MOTOR(0x26),
    MOTOR(0x27),
    TILT(0x28),
    NONE(0x00)
}

fun findBoostSensor(code: Byte) : BoostSensor {
    enumValues<BoostSensor>().forEach {
        if (it.code == code) {
            return it
        }
    }
    return BoostSensor.NONE
}

enum class TiltSensorOrientation(val code: Byte) {
    FLAT(0x00),
    STANDING_LED_UP(0x01),
    STANDING_BUTTON_UP(0x02),
    B_D_UP(0x03),
    A_C_UP(0x04),
    BATTERIES_UP(0x05),
    UNKNOWN(0xFF.toByte())
}

fun findTiltSensorOrientation(code: Byte) : TiltSensorOrientation {
    enumValues<TiltSensorOrientation>().forEach {
        if (it.code == code) {
            return it
        }
    }
    return TiltSensorOrientation.UNKNOWN
}