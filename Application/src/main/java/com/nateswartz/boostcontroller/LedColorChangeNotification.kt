package com.nateswartz.boostcontroller

class LedColorChangeNotification : HubNotification {
    constructor(str: String) : super(str)
    val color =  getColor("${rawData[12]}${rawData[13]}")

    override fun toString(): String {
        return "LED Color Change Notification - Color $color - $rawData"
    }
}