package com.nateswartz.boostcontroller

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import java.util.*


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class GattController(val moveHubService: MoveHubService) {

    val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")!!

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var boostHub: BluetoothDevice? = null
    private var handler: Handler? = null

    private var scanning = false
    private var found = false
    private var connected = false

    private var characteristic: BluetoothGattCharacteristic? = null

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.e(TAG, "Bluetooth Connected")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.e(TAG, "Bluetooth Disconnected")
                    val intentAction = ACTION_DEVICE_DISCONNECTED
                    connected = false
                    moveHubService.broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.e(TAG, "Bluetooth Services Discovered")
            val intentAction = ACTION_DEVICE_CONNECTED
            connected = true
            moveHubService.broadcastUpdate(intentAction)
            characteristic = bluetoothGatt!!.services!![2].characteristics[0]
            moveHubService.enableNotifications()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    moveHubService.handleNotification(data)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data.isNotEmpty()) {
                moveHubService.handleNotification(data)
            }
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        handler = Handler()

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = moveHubService.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        bluetoothAdapter = bluetoothManager!!.adapter
        bluetoothScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    fun connect() {
        if (!found && !scanning && !connected) {
            scanLeDevice(true)
        } else if (found && !scanning && !connected) {
            finishConnection()
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    private fun finishConnection(): Boolean {
        if (scanning) {
            bluetoothScanner!!.stopScan(leScanCallback)
            scanning = false
        }

        if (bluetoothAdapter == null || boostHub == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && boostHub!!.address == bluetoothDeviceAddress
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return (bluetoothGatt!!.connect())
        }

        val device = bluetoothAdapter!!.getRemoteDevice(boostHub!!.address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(moveHubService, false, gattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        bluetoothDeviceAddress = boostHub!!.address
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        if (scanning) {
            scanLeDevice(false)
        }
        if (connected) {
            bluetoothGatt!!.disconnect()
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt!!.close()
        bluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     * */
    private fun readCharacteristic() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(data: ByteArray): Boolean {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        // RGB LED WHITE
        characteristic!!.value = data
        return bluetoothGatt!!.writeCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(enabled: Boolean) {
        Log.e(TAG, "Enabling notifications")
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        // Check characteristic property
        val properties = characteristic!!.properties
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "PROPERY_NOTIFY is off")
        }

        bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        val descriptors = characteristic!!.descriptors

        for (descriptor in descriptors) {
            Log.e(TAG, descriptor.toString())
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt!!.writeDescriptor(descriptor)
            }
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler!!.postDelayed({
                Log.e(TAG, "Scanning timed out")
                scanning = false
                bluetoothScanner!!.stopScan(leScanCallback)
                val intentAction = ACTION_DEVICE_CONNECTION_FAILED
                moveHubService.broadcastUpdate(intentAction)
            }, DeviceControlActivity.SCAN_PERIOD)

            Log.e(TAG, "Scanning")
            scanning = true
            bluetoothScanner!!.startScan(
                    listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BoostUUID)).build()),
                    ScanSettings.Builder().build(),
                    leScanCallback)
            moveHubService.showMessage("Scanning...")

        } else {
            Log.e(TAG, "Stop Scanning")
            scanning = false
            bluetoothScanner!!.stopScan(leScanCallback)
            moveHubService.showMessage("Scanning Stopped")
        }
    }

    // Device scan callback.
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.e(TAG, result.toString())
            super.onScanResult(callbackType, result)
            boostHub = result.device
            found = true
            scanLeDevice(false)
            finishConnection()
        }
    }

    companion object {
        private val TAG = MoveHubService::class.java.simpleName

        val ACTION_DEVICE_CONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_GATT_CONNECTED"
        val ACTION_DEVICE_DISCONNECTED = "com.nateswartz.boostcontroller.move.hub.ACTION_GATT_DISCONNECTED"
        val ACTION_DEVICE_NOTIFICATION = "com.nateswartz.boostcontroller.move.hub.ACTION_DEVICE_NOTIFICATION"
        val ACTION_DEVICE_CONNECTION_FAILED = "com.nateswartz.boostcontroller.move.hub.ACTION_DEVICE_CONNECTION_FAILED"
        val NOTIFICATION_DATA = "com.nateswartz.boostcontroller.move.hub.NOTIFICATION_DATA"
    }
}
