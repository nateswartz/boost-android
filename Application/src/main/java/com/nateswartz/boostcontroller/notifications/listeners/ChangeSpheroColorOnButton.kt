package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService
import com.orbotix.ConvenienceRobot

class ChangeSpheroColorOnButton(private val legoBluetoothDeviceService: LegoBluetoothDeviceService, private val robot: ConvenienceRobot) : HubNotificationListener {

    override fun setup() {
        legoBluetoothDeviceService.moveHubController.activateButtonNotifications()
    }

    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val color = when ((1..4).shuffled().last()) {
                1 -> Triple(1.0f, 1.0f, 1.0f)
                2 -> Triple(0.0f, 1.0f, 0.0f)
                3 -> Triple(0.0f, 0.0f, 1.0f)
                else -> Triple(1.0f, 0.0f, 1.0f)
            }
            robot.setLed(color.first, color.second, color.third)
        }
    }

    override fun cleanup() {
        legoBluetoothDeviceService.moveHubController.deactivateButtonNotifications()
    }
}