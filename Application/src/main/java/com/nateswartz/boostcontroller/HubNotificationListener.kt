package com.nateswartz.boostcontroller

interface HubNotificationListener {
    fun checkNotification(notification: HubNotification): Boolean
    fun execute()
}

class RunMotorOnButtonClick(private val bluetoothDeviceService: BluetoothDeviceService) : HubNotificationListener {
    override fun checkNotification(notification: HubNotification): Boolean {
        return (notification is ButtonNotification && notification.buttonState == ButtonState.RELEASED)
    }

    override fun execute() {
        bluetoothDeviceService.moveHubController.runInternalMotors(50, 500, true)
    }
}

class ChangeLEDOnButtonClick(private val bluetoothDeviceService: BluetoothDeviceService) : HubNotificationListener {
    override fun checkNotification(notification: HubNotification): Boolean {
        return (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED)
    }
    override fun execute() {
        val color = when ((1..3).shuffled().last()) {
            1 -> LEDColorCommand.RED
            2 -> LEDColorCommand.GREEN
            else -> LEDColorCommand.ORANGE
        }
        bluetoothDeviceService.moveHubController.setLEDColor(color)
    }
}