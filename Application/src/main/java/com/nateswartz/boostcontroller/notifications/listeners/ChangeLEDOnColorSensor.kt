package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.misc.getLedColorFromName
import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService

class ChangeLEDOnColorSensor(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    override fun setup() {
        legoBluetoothDeviceService.moveHubController.activateColorSensorNotifications()
    }

    override fun execute(notification: HubNotification) {
        if (notification is ColorSensorNotification) {
            legoBluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(notification.color.string))
        }
    }

    override fun cleanup() {
        legoBluetoothDeviceService.moveHubController.deactivateColorSensorNotifications()
    }
}
