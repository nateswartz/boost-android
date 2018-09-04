package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.enums.LEDColorCommand
import com.nateswartz.boostcontroller.notifications.AdvancedTiltSensorNotification
import com.nateswartz.boostcontroller.notifications.HubNotification
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService

class ChangeLEDColorOnTilt(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    private var currentOrientationValue = ""

    override fun setup() {
        legoBluetoothDeviceService.moveHubController.activateAdvancedTiltSensorNotifications()
    }

    override fun execute(notification: HubNotification) {
        if (notification is AdvancedTiltSensorNotification && notification.orientation != currentOrientationValue) {
            val color = when ((1..6).shuffled().last()) {
                1 -> LEDColorCommand.RED
                2 -> LEDColorCommand.GREEN
                3 -> LEDColorCommand.PINK
                4 -> LEDColorCommand.PURPLE
                5 -> LEDColorCommand.LIGHTBLUE
                else -> LEDColorCommand.ORANGE
            }
            legoBluetoothDeviceService.moveHubController.setLEDColor(color)
            currentOrientationValue = notification.orientation
        }
    }

    override fun cleanup() {
        legoBluetoothDeviceService.moveHubController.deactivateAdvancedTiltSensorNotifications()
    }
}