package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService

class RunMotorOnButtonClick(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.RELEASED) {
            legoBluetoothDeviceService.moveHubController.runInternalMotors(50, 500, true)
        }
    }
}