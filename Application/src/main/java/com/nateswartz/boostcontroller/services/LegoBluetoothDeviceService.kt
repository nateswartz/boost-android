package com.nateswartz.boostcontroller.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.nateswartz.boostcontroller.controllers.GattController
import com.nateswartz.boostcontroller.notifications.HubNotificationFactory
import com.nateswartz.boostcontroller.controllers.MoveHubController
import com.nateswartz.boostcontroller.enums.BoostSensor
import com.nateswartz.boostcontroller.notifications.HubNotification
import com.nateswartz.boostcontroller.notifications.PortConnectedNotification


interface BluetoothGattNotifier {
    fun broadcastUpdate(action: String)
    fun handleNotification(data: ByteArray)
    fun getContext(): Context
}

class LegoBluetoothDeviceService : Service(), BluetoothGattNotifier {

    private var gattController = GattController(this)
    var moveHubController = MoveHubController(gattController)

    override fun getContext(): Context {
        return applicationContext
    }

    override fun handleNotification(data: ByteArray) {
        val notification = HubNotificationFactory.build(data)
        if (notification is PortConnectedNotification) {
            when (notification.sensor) {
                BoostSensor.DISTANCE_COLOR -> {
                    moveHubController.colorSensorPort = notification.port
                    HubNotificationFactory.ColorSensorPort = notification.port
                }
                BoostSensor.EXTERNAL_MOTOR -> {
                    moveHubController.externalMotorPort = notification.port
                    HubNotificationFactory.ExternalMotorPort = notification.port
                }
                else -> Log.w(TAG, "Unknown sensor")
            }
        }
        Log.d(TAG, notification.toString())
        val intentAction = ACTION_DEVICE_NOTIFICATION
        broadcastUpdate(intentAction, notification)
    }

    /*fun dumpData() {
        for (service in bluetoothGatt!!.services) {
            Log.e(TAG, "Service: ${service.uuid}")
            if (service.characteristics != null) {
                for (characteristic in service.characteristics) {
                    Log.e(TAG, "Characteristic: ${characteristic.uuid}")
                    if (characteristic.value != null) {
                        for (byte in characteristic.value) {
                            Log.e(TAG, String.format("%02X", byte))
                        }
                    }
                    if (characteristic.descriptors != null) {
                        for (descriptor in characteristic.descriptors) {
                            Log.e(TAG, "Descriptor: ${descriptor.uuid}")
                            if (descriptor.value != null) {
                                for (byte in descriptor.value) {
                                    Log.e(TAG, String.format("%02X", byte))
                                }
                            }
                        }
                    }
                }
            }
        }
    }*/

    private val binder = LocalBinder()

    override fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, notification: HubNotification) {
        val intent = Intent(action)
        intent.putExtra(NOTIFICATION_DATA, notification)
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: LegoBluetoothDeviceService
            get() = this@LegoBluetoothDeviceService
    }

    fun connect() {
        Log.d(TAG, "Connecting...")
        gattController.connect()
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        gattController.disconnect()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        gattController.initialize()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        Log.d(TAG, "onUnbind")
        gattController.close()
        return super.onUnbind(intent)
    }

    companion object {
        private val TAG = LegoBluetoothDeviceService::class.java.simpleName

        const val ACTION_BOOST_CONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_BOOST_CONNECTED"
        const val ACTION_BOOST_DISCONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_BOOST_DISCONNECTED"

        const val ACTION_LPF2_CONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_LPF2_CONNECTED"
        const val ACTION_LPF2_DISCONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_LPF2_DISCONNECTED"

        const val ACTION_DEVICE_CONNECTION_FAILED = "com.nateswartz.boostcontroller.move.hub.ACTION_DEVICE_CONNECTION_FAILED"

        const val ACTION_DEVICE_NOTIFICATION = "com.nateswartz.boostcontroller.move.hub.ACTION_DEVICE_NOTIFICATION"
        const val NOTIFICATION_DATA = "com.nateswartz.boostcontroller.move.hub.NOTIFICATION_DATA"
    }
}
