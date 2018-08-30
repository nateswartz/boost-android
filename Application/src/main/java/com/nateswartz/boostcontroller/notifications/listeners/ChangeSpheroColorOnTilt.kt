package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.enums.SpheroColors
import com.nateswartz.boostcontroller.notifications.HubNotification
import com.nateswartz.boostcontroller.notifications.Orientation
import com.nateswartz.boostcontroller.notifications.TiltSensorNotification
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService
import com.orbotix.ConvenienceRobot

class ChangeSpheroColorOnTilt(private val legoBluetoothDeviceService: LegoBluetoothDeviceService, private val robot: ConvenienceRobot) : HubNotificationListener {

    override fun setup() {
        legoBluetoothDeviceService.moveHubController.activateTiltSensorNotifications()
    }

    override fun execute(notification: HubNotification) {
        if (notification is TiltSensorNotification) {
            val color = when (notification.orientation) {
                Orientation.FLAT -> SpheroColors.RED.data
                Orientation.STANDING_LED_UP -> SpheroColors.GREEN.data
                Orientation.STANDING_BUTTON_UP -> SpheroColors.BLUE.data
                Orientation.B_D_UP -> SpheroColors.YELLOW.data
                Orientation.A_C_UP -> SpheroColors.PURPLE.data
                Orientation.BATTERIES_UP -> SpheroColors.WHITE.data
                Orientation.UNKNOWN -> SpheroColors.BLACK.data
            }
            robot.setLed(color.first, color.second, color.third)
        }
    }

    override fun cleanup() {
        legoBluetoothDeviceService.moveHubController.deactivateTiltSensorNotifications()
    }
}