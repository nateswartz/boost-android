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
class MoveHubService : Service() {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDeviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    private var characteristic: BluetoothGattCharacteristic? = null

    private val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
    private val ACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)

    private val C_PORT_BYTE = 0x01.toByte()
    private val D_PORT_BYTE = 0x02.toByte()
    private val AB_PORT_BYTE = 0x39.toByte()
    private val A_PORT_BYTE = 0x37.toByte()
    private val B_PORT_BYTE = 0x38.toByte()

    private var ColorSensorPort = ""
    private var ExternalMotorPort = ""

    fun setLEDColor(color: LEDColorCommand) {
        writeCharacteristic(characteristic!!, color.data)
    }

    fun runExternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        var portByte : Byte? = null
        if (ExternalMotorPort == "C") {
            portByte = C_PORT_BYTE
        } else if (ExternalMotorPort == "D") {
            portByte = D_PORT_BYTE
        }
        runMotor(powerPercentage, timeInMilliseconds, counterclockwise, portByte!!)
    }

    fun runInternalMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, motor: String)
    {
        when (motor) {
            "A" -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, A_PORT_BYTE)
            "B" -> runMotor(powerPercentage, timeInMilliseconds, counterclockwise, B_PORT_BYTE)
        }
    }

    fun runInternalMotors(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean) {
        runMotor(powerPercentage, timeInMilliseconds, counterclockwise, AB_PORT_BYTE)
    }

    fun runInternalMotorsInOpposition(powerPercentage: Int, timeInMilliseconds: Int) {
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val motorAPower = powerPercentage.toByte()
        val motorBPower = (255 - powerPercentage).toByte()
        val RUN_MOTOR = byteArrayOf(0x0d, 0x00, 0x81.toByte(), AB_PORT_BYTE, 0x11, 0x0a, timeBytes[0], timeBytes[1], motorAPower, motorBPower, 0x64, 0x7f, 0x03)
        writeCharacteristic(characteristic!!, RUN_MOTOR)
    }

    fun enableNotifications() {
        setCharacteristicNotification(characteristic!!, true)
    }

    fun activateButtonNotifications() {
        writeCharacteristic(characteristic!!, ACTIVATE_BUTTON)
    }

    fun activateColorSensorNotifications() {
        when (ColorSensorPort) {
            "C" -> writeCharacteristic(characteristic!!, ACTIVATE_COLOR_SENSOR_PORT_C)
            "D" -> writeCharacteristic(characteristic!!, ACTIVATE_COLOR_SENSOR_PORT_D)
        }
    }

    fun activateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            "C" -> writeCharacteristic(characteristic!!, ACTIVATE_EXTERNAL_MOTOR_PORT_C)
            "D" -> writeCharacteristic(characteristic!!, ACTIVATE_EXTERNAL_MOTOR_PORT_D)
        }
    }

    fun activateInternalMotorSensorsNotifications() {
        activateInternalMotorSensorNotifications("A")
        activateInternalMotorSensorNotifications("B")
    }

    fun activateInternalMotorSensorNotifications(motor: String) {
        var data = ACTIVATE_MOTOR_PORT
        when (motor) {
            "A" -> data[3] = A_PORT_BYTE
            "B" -> data[3] = B_PORT_BYTE
        }
        writeCharacteristic(characteristic!!, data)
    }

    fun activateTiltSensorNotifications() {
        writeCharacteristic(characteristic!!, ACTIVATE_TILT_SENSOR)
    }

    private fun handleNotification(data: ByteArray) {
        val stringBuilder = StringBuilder(data.size)
        for (byteChar in data)
            stringBuilder.append(String.format("%02X ", byteChar))
        val pieces = (String(data) + "\n" + stringBuilder.toString()).split("\n")
        val encodedData = pieces[pieces.size - 1]
        val notification = HubNotificationFactory.build(encodedData.trim())
        if (notification is PortInfoNotification) {
            when(notification.sensor) {
                "DistanceColor" -> {
                    ColorSensorPort = notification.port
                    HubNotificationFactory.ColorSensorPort = notification.port
                }
                "ExternalMotor" -> {
                    ExternalMotorPort = notification.port
                    HubNotificationFactory.ExternalMotorPort = notification.port
                }
            }
        }
        Log.e(TAG, notification.toString())
    }

    private fun runMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, portByte: Byte) {
        val powerByte = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val RUN_MOTOR = byteArrayOf(0x0c, 0x00, 0x81.toByte(), portByte, 0x11, 0x09, timeBytes[0], timeBytes[1], powerByte, 0x64, 0x7f, 0x03)
        writeCharacteristic(characteristic!!, RUN_MOTOR)
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
                characteristic = supportedGattServices!![2].characteristics[0]
                enableNotifications()
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    handleNotification(data)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data.isNotEmpty()) {
                handleNotification(data)
            }
        }
    }

    private val binder = LocalBinder()

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    private val supportedGattServices: List<BluetoothGattService>?
        get() = if (bluetoothGatt == null) null else bluetoothGatt!!.services

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: MoveHubService
            get() = this@MoveHubService
    }

    override fun onBind(intent: Intent): IBinder? {
        initialize()
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
    private fun initialize(): Boolean {
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
    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        bluetoothGatt!!.readCharacteristic(characteristic)
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray): Boolean {
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
    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
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

    companion object {
        private val TAG = MoveHubService::class.java.simpleName

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2

        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    }
}
