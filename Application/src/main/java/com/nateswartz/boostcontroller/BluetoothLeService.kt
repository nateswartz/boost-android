package com.nateswartz.boostcontroller

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                connectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + bluetoothGatt!!.discoverServices())

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                connectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private val binder = LocalBinder()

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (bluetoothGatt == null) null else bluetoothGatt!!.services

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String,
                                characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // For all other profiles, writes the data formatted in HEX.
        val data = characteristic.value
        if (data.isNotEmpty()) {
            val stringBuilder = StringBuilder(data.size)
            for (byteChar in data)
                stringBuilder.append(String.format("%02X ", byteChar))
            intent.putExtra(EXTRA_DATA, String(data) + "\n" + stringBuilder.toString())
        }
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        bluetoothAdapter = bluetoothManager!!.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
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
    fun connect(address: String?): Boolean {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address == bluetoothDeviceAddress
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (bluetoothGatt!!.connect()) {
                connectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }

        val device = bluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        bluetoothDeviceAddress = address
        connectionState = STATE_CONNECTING
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
        bluetoothGatt!!.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private fun close() {
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
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray): Boolean {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        // RGB LED WHITE
        characteristic.value = data
        return bluetoothGatt!!.writeCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                      enabled: Boolean) {
        Log.e(TAG, "Enabling notifications")
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        // Check characteristic property
        val properties = characteristic.properties
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "PROPERY_NOTIFY is off")
        }

        bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        val descriptors = characteristic.descriptors

        for (descriptor in descriptors) {
            Log.e(TAG, descriptor.toString())
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt!!.writeDescriptor(descriptor)
            }
        }
    }

    fun dumpData() {
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
    }

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2

        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    }
}
