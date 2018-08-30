package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.notifications.*

interface HubNotificationListener {
    fun execute(notification: HubNotification)
    fun setup()
    fun cleanup()
}