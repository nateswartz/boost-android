package com.nateswartz.boostcontroller.notifications.listeners

import com.nateswartz.boostcontroller.controllers.LifxController
import com.nateswartz.boostcontroller.enums.LEDColorCommand
import com.nateswartz.boostcontroller.misc.getLedColorFromName
import com.nateswartz.boostcontroller.notifications.*
import com.nateswartz.boostcontroller.services.LegoBluetoothDeviceService
import com.orbotix.ConvenienceRobot
import kotlin.math.absoluteValue

interface HubNotificationListener {
    fun execute(notification: HubNotification)
}

class RunMotorOnButtonClick(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.RELEASED) {
            legoBluetoothDeviceService.moveHubController.runInternalMotors(50, 500, true)
        }
    }
}

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

class ChangeLEDOnColorSensor(private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    override fun execute(notification: HubNotification) {
        if (notification is ColorSensorNotification) {
            legoBluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(notification.color.string))
        }
    }
}

class ChangeLifxLEDOnMotorButton(private val legoBluetoothDeviceService: LegoBluetoothDeviceService, private val lifxController: LifxController) : HubNotificationListener {
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
                legoBluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(colors[currentColorIndex]))
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

class RollerCoaster(private val time: String, private val counterclockwise: Boolean, private val legoBluetoothDeviceService: LegoBluetoothDeviceService) : HubNotificationListener {
    private var currentRotationValue = 0
    private var colors = arrayOf("red", "blue", "green")
    private var currentColorIndex = 0

    override fun execute(notification: HubNotification) {
        if (notification is ButtonNotification && notification.buttonState == ButtonState.PRESSED) {
            val power = when (currentColorIndex) {
                0 -> 10
                1 -> 20
                else -> 30
            }
            legoBluetoothDeviceService.moveHubController.runExternalMotor(power, time.toInt(), counterclockwise)
        }
        if (notification is InternalMotorNotification && (notification.port == Port.A || notification.port == Port.B)) {
            val rotationValue = notification.rotationValue

            if ((rotationValue - currentRotationValue).absoluteValue > 150) {
                if (rotationValue > currentRotationValue) {
                    currentColorIndex = if (currentColorIndex == colors.size - 1) 0 else currentColorIndex + 1
                } else if (currentRotationValue > rotationValue) {
                    currentColorIndex = if (currentColorIndex == 0) colors.size - 1 else currentColorIndex - 1
                }
                legoBluetoothDeviceService.moveHubController.setLEDColor(getLedColorFromName(colors[currentColorIndex]))
                currentRotationValue = rotationValue
            }
        }
    }
}