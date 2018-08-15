package com.nateswartz.boostcontroller

import com.orbotix.ConvenienceRobot
import kotlin.math.absoluteValue

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
            bluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(notification.color.string))
        }
    }
}

class ChangeLifxLEDOnMotorButton(private val bluetoothDeviceService: BluetoothDeviceService, private val lifxController: LifxController) : HubNotificationListener {
    private var currentRotationValue = 0
    private var colors = arrayOf("kelvin:3200", "red", "blue", "purple", "green")
    private var currentColorIndex = 0

    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            lifxController.changeLightColor(colors[currentColorIndex])
        }
        if (notification is InternalMotorNotification) {
            val rotationValue = notification.rotationValue

            if ((rotationValue - currentRotationValue).absoluteValue > 100) {
                if (rotationValue > currentRotationValue) {
                    currentColorIndex = if (currentColorIndex == colors.size - 1) 0 else currentColorIndex + 1
                } else if (currentRotationValue > rotationValue) {
                    currentColorIndex = if (currentColorIndex == 0) colors.size - 1 else currentColorIndex - 1
                }
                bluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(colors[currentColorIndex]))
                currentRotationValue = rotationValue
            }
        }
    }
}

class ChangeSpheroColorOnButton(private val robot: ConvenienceRobot) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val color = when ((1..4).shuffled().last()) {
                1 -> Triple(1.0f, 1.0f, 1.0f)
                2 -> Triple(0.0f, 1.0f, 0.0f)
                3 -> Triple(0.0f, 0.0f, 1.0f)
                else -> Triple(1.0f, 0.0f, 1.0f)
            }
            robot.setLed(color.first, color.second, color.third)
        }
    }
}