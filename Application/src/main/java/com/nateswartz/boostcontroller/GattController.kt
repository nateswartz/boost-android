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


class GattController(val moveHubService: MoveHubService) {

    val BoostUUID = UUID.fromString("00001623-1212-efde-1623-785feabcd123")!!
    val BoostHubManufacturerData = "64"
    val Lpf2HubManufacturerData = "65"

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var handler: Handler? = null

    private var scanning = false

    private var boostHub: BluetoothDevice? = null
    private var boostHubAddress = ""
    private var boostCharacteristic: BluetoothGattCharacteristic? = null
    private var foundBoost = false
    private var connectedBoost = false

    private var lpf2Hub: BluetoothDevice? = null
    private var lpf2HubAddress = ""
    private var lpf2Characteristic: BluetoothGattCharacteristic? = null
    private var foundLpf2 = false
    private var connectedLpf2 = false

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
                    val intentAction : String
                    when (gatt.device.address) {
                        boostHubAddress -> {
                            connectedBoost = false
                            intentAction = MoveHubService.ACTION_BOOST_DISCONNECTED
                        }
                        else -> {
                            connectedLpf2 = false
                            intentAction = MoveHubService.ACTION_LPF2_DISCONNECTED

                        }
                    }
                    moveHubService.broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.e(TAG, "Bluetooth Services Discovered")
            val intentAction : String
            when (gatt.device.address) {
                boostHubAddress -> {
                    Log.e(TAG, "Connected Boost Hub")
                    connectedBoost = true
                    boostCharacteristic = bluetoothGatt!!.services!![2].characteristics[0]
                    intentAction = MoveHubService.ACTION_BOOST_CONNECTED
                }
                else -> {
                    Log.e(TAG, "Connected LPF2 Hub")
                    connectedLpf2 = true
                    // TODO: Verify this is the correct characteristic
                    lpf2Characteristic = bluetoothGatt!!.services!![2].characteristics[0]
                    intentAction = MoveHubService.ACTION_LPF2_CONNECTED
                }
            }
            moveHubService.broadcastUpdate(intentAction)
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
        if (!foundBoost && !scanning && !connectedBoost) {
            scanLeDevice(true)
        } else if (foundBoost && !scanning && !connectedBoost) {
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
        if (connectedBoost) {
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
        bluetoothGatt!!.readCharacteristic(boostCharacteristic)
    }

    fun writeCharacteristic(data: ByteArray): Boolean {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        boostCharacteristic!!.value = data
        return bluetoothGatt!!.writeCharacteristic(boostCharacteristic)
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
        val properties = boostCharacteristic!!.properties
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "PROPERY_NOTIFY is off")
        }

        bluetoothGatt!!.setCharacteristicNotification(boostCharacteristic, enabled)

        val descriptors = boostCharacteristic!!.descriptors

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
                val intentAction = MoveHubService.ACTION_DEVICE_CONNECTION_FAILED
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
            when (result.scanRecord.manufacturerSpecificData[919][1].toString()) {
                BoostHubManufacturerData -> {
                    boostHub = result.device
                    boostHubAddress = result.device.address
                    foundBoost = true
                    Log.e(TAG, "Found Boost Hub")
                }
                Lpf2HubManufacturerData -> {
                    lpf2Hub = result.device
                    lpf2HubAddress = result.device.address
                    foundLpf2 = true
                    Log.e(TAG, "Found LPF2 Hub")
                }
            }
            scanLeDevice(false)
            finishConnection()
        }
    }

    companion object {
        private val TAG = GattController::class.java.simpleName
    }
}
