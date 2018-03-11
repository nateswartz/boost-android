package com.nateswartz.boostcontroller

open class HubNotification(var rawData: String) {
    override fun toString(): String {
        return "Hub Notification - $rawData"
    }
}
