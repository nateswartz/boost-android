package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.enums.LEDColorCommand
import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService

class ChangeLEDOnButtonClick(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val color = when ((1..5).shuffled().last()) {
                1 -> LEDColorCommand.RED
                2 -> LEDColorCommand.GREEN
                3 -> LEDColorCommand.PINK
                4 -> LEDColorCommand.PURPLE
                else -> LEDColorCommand.ORANGE
            }
            legoBluetoothDeviceService.moveHubController.setLEDColor(color)
        }
    }
}
