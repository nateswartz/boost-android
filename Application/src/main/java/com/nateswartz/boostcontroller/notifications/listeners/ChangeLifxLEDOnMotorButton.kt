package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.controllers.LifxController
import com.nateswartz.boostcontroller.misc.getLedColorFromName
import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService
import kotlin.math.absoluteValue

class ChangeLifxLEDOnMotorButton(private val legoBluetoothDeviceService: LegoBluetoothDeviceService, private val lifxController: LifxController) : HubNotificationListener {
    private var currentRotationValue = 0
    private var colors = arrayOf("kelvin:3200", "red", "blue", "purple", "green")
    private var currentColorIndex = 0

    override fun setup() {
        legoBluetoothDeviceService.moveHubController.activateButtonNotifications()
        legoBluetoothDeviceService.moveHubController.activateInternalMotorSensorsNotifications()
    }

    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            lifxController.changeLightColor(colors[currentColorIndex])
        }
        if (notification is InternalMotorNotification) {
            val rotationValue = notification.rotationValue

            if ((rotationValue - currentRotationValue).absoluteValue > 100) {
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

    override fun cleanup() {
        legoBluetoothDeviceService.moveHubController.deactivateButtonNotifications()
        legoBluetoothDeviceService.moveHubController.deactivateInternalMotorSensorsNotifications()
    }
}