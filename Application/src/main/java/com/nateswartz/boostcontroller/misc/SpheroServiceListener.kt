package com.nateswartz.boostcontroller.misc

import com.orbotix.ConvenienceRobot
import com.orbotix.common.RobotChangedStateListener

interface SpheroServiceListener {
    fun handleSpheroChange(robot: ConvenienceRobot, type: RobotChangedStateListener.RobotChangedStateNotificationType)
}