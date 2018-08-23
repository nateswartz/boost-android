package com.nateswartz.boostcontroller

import com.orbotix.ConvenienceRobot
import com.orbotix.common.RobotChangedStateListener

interface SpheroServiceListener {
    fun handleSpheroChange(robot: ConvenienceRobot, type: RobotChangedStateListener.RobotChangedStateNotificationType)
}