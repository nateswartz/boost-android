package com.nateswartz.boostcontroller

interface HubNotificationListener {
    fun execute(notification: HubNotification)
}

class RunMotorOnButtonClick(private val bluetoothDeviceService: BluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.RELEASED) {
            bluetoothDeviceService.moveHubController.runInternalMotors(50, 500, true)
        }
    }
}

class ChangeLEDOnButtonClick(private val bluetoothDeviceService: BluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val color = when ((1..5).shuffled().last()) {
                1 -> LEDColorCommand.RED
                2 -> LEDColorCommand.GREEN
                3 -> LEDColorCommand.PINK
                4 -> LEDColorCommand.PURPLE
                else -> LEDColorCommand.ORANGE
            }
            bluetoothDeviceService.moveHubController.setLEDColor(color)
        }
    }
}

class ChangeLEDOnColorSensor(private val bluetoothDeviceService: BluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ColorSensorNotification) {
            bluetoothDeviceService!!.moveHubController.setLEDColor(getLedColorFromName(notification.color.string))
        }
    }
}