package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.misc.getLedColorFromName
import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService
import kotlin.math.absoluteValue

class RollerCoaster(private val time: String, private val counterclockwise: Boolean, private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    private var currentRotationValue = 0
    private var colors = arrayOf("red", "blue", "green")
    private var currentColorIndex = 0

    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val power = when (currentColorIndex) {
                0 -> 10
                1 -> 20
                else -> 30
            }
            legoBluetoothDeviceService.moveHubController.runExternalMotor(power, time.toInt(), counterclockwise)
        }
        if (notification is InternalMotorNotification && (notification.port == Port.A || notification.port == Port.B)) {
            val rotationValue = notification.rotationValue

            if ((rotationValue - currentRotationValue).absoluteValue > 150) {
                if (rotationValue > currentRotationValue) {
                    currentColorIndex = if (currentColorIndex == colors.size - 1) 0 else currentColorIndex + 1
                } else if (currentRotationValue > rotationValue) {
                    currentColorIndex = if (currentColorIndex == 0) colors.size - 1 else currentColorIndex - 1
                }
                legoBluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(colors[currentColorIndex]))
                currentRotationValue = rotationValue
            }
        }
    }
}