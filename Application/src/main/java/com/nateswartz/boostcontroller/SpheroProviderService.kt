package com.nateswartz.boostcontroller

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.orbotix.ConvenienceRobot
import com.orbotix.DualStackDiscoveryAgent
import com.orbotix.common.DiscoveryException
import com.orbotix.common.Robot
import com.orbotix.common.RobotChangedStateListener


class SpheroProviderService : Service(), RobotChangedStateListener {
    private var listeners: MutableList<SpheroServiceListener> = mutableListOf()
    private val discoveryAgent = DualStackDiscoveryAgent()
    private var sphero: ConvenienceRobot? = null
    private val binder = RobotBinder()

    inner class RobotBinder : Binder() {
        internal val service: SpheroProviderService
            get() = this@SpheroProviderService
    }

    override fun onCreate() {
        Log.e("Service", "onCreate")
        discoveryAgent.addRobotStateListener(this)
    }

    override fun onDestroy() {
        Log.e("Service", "onDestroy")
        //If the DiscoveryAgent is in discovery mode, stop it.
        if (discoveryAgent.isDiscovering) {
            discoveryAgent.stopDiscovery()
        }

        //If a sphero is connected to the device, disconnect it
        sphero?.disconnect()
        sphero = null
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e("Service", "onBind")
        if (!discoveryAgent.isDiscovering) {
            try {
                Log.e("Service", "Discovering...")
                discoveryAgent.startDiscovery(applicationContext)
            } catch (e: DiscoveryException) {
                Log.e("Service", "DiscoveryException: " + e.message)
            }
        }
        return binder
    }

    override fun handleRobotChangedState(robot: Robot, type: RobotChangedStateListener.RobotChangedStateNotificationType) {
        Log.e("Service", "handleRobotChangedState $type")
        when (type)
        {
            RobotChangedStateListener.RobotChangedStateNotificationType.Online -> this.sphero = ConvenienceRobot(robot)
        }
        for (listener in listeners) {
            listener.handleSpheroChange(ConvenienceRobot(robot), type)
        }
    }

    fun hasActiveSphero() : Boolean {
        return sphero?.isConnected == true
    }

    fun getSphero() : ConvenienceRobot {
        return sphero!!
    }

    fun addListener(listener: SpheroServiceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SpheroServiceListener) {
        listeners.remove(listener)
    }
}