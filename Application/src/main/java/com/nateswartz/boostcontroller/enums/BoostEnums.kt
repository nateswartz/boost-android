package com.nateswartz.boostcontroller.enums

enum class MotorNotificationType {
    ANGLE, SPEED, NONE
}

enum class InternalMotorPort {
    A, B, A_B, NONE
}

enum class BoostPort {
    A, B, A_B, C, D, NONE
}

enum class ExternalSensorPort {
    C, D, NONE
}

enum class ExternalSensorType {
    COLOR, MOTOR, NONE
}

enum class BoostSensor {
    LED, DISTANCE_COLOR, EXTERNAL_MOTOR, MOTOR, TILT, NONE
}

enum class TiltSensorOrientation {
    FLAT,
    STANDING_LED_UP,
    STANDING_BUTTON_UP,
    B_D_UP,
    A_C_UP,
    BATTERIES_UP,
    UNKNOWN
}