package com.nateswartz.boostcontroller

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import java.util.*

enum class DeviceType {
    BOOST, LPF2
}

/*
Handle to send data to:
attr handle: 0x000c, end grp handle: 0x000f uuid: 00001623-1212-efde-1623-785feabcd123

handle: 0x000d, char properties: 0x1e, char value handle: 0x000e, uuid: 00001624-1212-efde-1623-785feabcd123
*/
class GattController(val bluetoothDeviceService: BluetoothDeviceService) {

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
                    Log.d(TAG, "Bluetooth Connected")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Bluetooth Disconnected")
                    val intentAction : String
                    when (gatt.device.address) {
                        boostHubAddress -> {
                            connectedBoost = false
                            intentAction = BluetoothDeviceService.ACTION_BOOST_DISCONNECTED
                        }
                        else -> {
                            connectedLpf2 = false
                            intentAction = BluetoothDeviceService.ACTION_LPF2_DISCONNECTED

                        }
                    }
                    bluetoothDeviceService.broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "Bluetooth Services Discovered")
            val intentAction : String
            when (gatt.device.address) {
                boostHubAddress -> {
                    Log.d(TAG, "Connected Boost Hub")
                    connectedBoost = true
                    boostCharacteristic = bluetoothGatt!!.services!![2].characteristics[0]
                    intentAction = BluetoothDeviceService.ACTION_BOOST_CONNECTED
                }
                else -> {
                    Log.d(TAG, "Connected LPF2 Hub")
                    connectedLpf2 = true
                    // TODO: Verify this is the correct characteristic
                    lpf2Characteristic = bluetoothGatt!!.services!![2].characteristics[0]
                    intentAction = BluetoothDeviceService.ACTION_LPF2_CONNECTED
                }
            }
            bluetoothDeviceService.broadcastUpdate(intentAction)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    bluetoothDeviceService.handleNotification(data)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data.isNotEmpty()) {
                bluetoothDeviceService.handleNotification(data)
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
            bluetoothManager = bluetoothDeviceService.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
        Log.d(TAG, "Connecting...")
        if ( !scanning && (!foundBoost || !foundLpf2) && (!connectedLpf2 || !connectedBoost)) {
            Log.d(TAG, "Scanning...")
            scanLeDevice(true)
        } else if (!scanning && foundBoost && foundLpf2 && (!connectedBoost || !connectedLpf2)) {
            Log.d(TAG, "Finishing connection...")
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
            Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.")
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
            Log.e(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(bluetoothDeviceService, false, gattCallback)
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
            Log.e(TAG, "BluetoothAdapter not initialized")
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
    private fun readCharacteristic(deviceType: DeviceType) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return
        }
        val characteristic = if (deviceType == DeviceType.BOOST) boostCharacteristic else lpf2Characteristic
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(deviceType: DeviceType, data: ByteArray): Boolean {
        Log.d(TAG, "Writing characteristic: ${convertBytesToString(data)}")
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return false
        }
        val characteristic = if (deviceType == DeviceType.BOOST) boostCharacteristic else lpf2Characteristic
        characteristic!!.value = data
        return bluetoothGatt!!.writeCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(deviceType: DeviceType, enabled: Boolean) {
        Log.d(TAG, "Enabling notifications")
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return
        }
        val characteristic = if (deviceType == DeviceType.BOOST) boostCharacteristic else lpf2Characteristic

        // Check characteristic property
        val properties = characteristic!!.properties
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "PROPERY_NOTIFY is off")
        }

        bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        val descriptors = characteristic!!.descriptors

        for (descriptor in descriptors) {
            Log.d(TAG, descriptor.toString())
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
                Log.d(TAG, "Scanning timed out")
                scanning = false
                bluetoothScanner!!.stopScan(leScanCallback)
                val intentAction = BluetoothDeviceService.ACTION_DEVICE_CONNECTION_FAILED
                bluetoothDeviceService.broadcastUpdate(intentAction)
            }, DeviceControlActivity.SCAN_PERIOD)

            Log.d(TAG, "Scanning")
            scanning = true
            bluetoothScanner!!.startScan(
                    listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BoostUUID)).build()),
                    ScanSettings.Builder().build(),
                    leScanCallback)
            bluetoothDeviceService.showMessage("Scanning...")

        } else {
            Log.d(TAG, "Stop Scanning")
            scanning = false
            bluetoothScanner!!.stopScan(leScanCallback)
            bluetoothDeviceService.showMessage("Scanning Stopped")
        }
    }

    // Device scan callback.
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, result.toString())
            super.onScanResult(callbackType, result)
            when (result.scanRecord.manufacturerSpecificData[919][1].toString()) {
                BoostHubManufacturerData -> {
                    boostHub = result.device
                    boostHubAddress = result.device.address
                    foundBoost = true
                    Log.d(TAG, "Found Boost Hub")
                }
                Lpf2HubManufacturerData -> {
                    lpf2Hub = result.device
                    lpf2HubAddress = result.device.address
                    foundLpf2 = true
                    Log.d(TAG, "Found LPF2 Hub")
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
