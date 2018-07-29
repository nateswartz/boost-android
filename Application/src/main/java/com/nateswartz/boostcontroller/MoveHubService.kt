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
class MoveHubService : Service() {

    private var gattController = GattController(this)

    private val ACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x01, 0x02, 0x02)
    // Currently not working
    private val DEACTIVATE_BUTTON = byteArrayOf(0x05, 0x00, 0x03, 0x02, 0x00)

    private val ACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val DEACTIVATE_COLOR_SENSOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00)
    private val DEACTIVATE_COLOR_SENSOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00)

    private val ACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val ACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val DEACTIVATE_EXTERNAL_MOTOR_PORT_C = byteArrayOf(0x0a, 0x00, 0x41, 0x01, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)
    private val DEACTIVATE_EXTERNAL_MOTOR_PORT_D = byteArrayOf(0x0a, 0x00, 0x41, 0x02, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)

    private val ACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val DEACTIVATE_MOTOR_PORT = byteArrayOf(0x0a, 0x00, 0x41, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)

    private val ACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x01)
    private val DEACTIVATE_TILT_SENSOR = byteArrayOf(0x0a, 0x00, 0x41, 0x3a, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00)

    private val C_PORT_BYTE = 0x01.toByte()
    private val D_PORT_BYTE = 0x02.toByte()
    private val AB_PORT_BYTE = 0x39.toByte()
    private val A_PORT_BYTE = 0x37.toByte()
    private val B_PORT_BYTE = 0x38.toByte()

    private var ColorSensorPort = ""
    private var ExternalMotorPort = ""

    fun setLEDColor(color: LEDColorCommand) {
        gattController.writeCharacteristic(color.data)
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
        gattController.writeCharacteristic(RUN_MOTOR)
    }

    fun enableNotifications() {
        gattController.setCharacteristicNotification(true)
    }

    fun activateButtonNotifications() {
        gattController.writeCharacteristic(ACTIVATE_BUTTON)
    }

    // Currently not working
    fun deactivateButtonNotifications() {
        gattController.writeCharacteristic(DEACTIVATE_BUTTON)
    }

    fun activateColorSensorNotifications() {
        when (ColorSensorPort) {
            "C" -> gattController.writeCharacteristic(ACTIVATE_COLOR_SENSOR_PORT_C)
            "D" -> gattController.writeCharacteristic(ACTIVATE_COLOR_SENSOR_PORT_D)
        }
    }

    fun deactivateColorSensorNotifications() {
        when (ColorSensorPort) {
            "C" -> gattController.writeCharacteristic(DEACTIVATE_COLOR_SENSOR_PORT_C)
            "D" -> gattController.writeCharacteristic(DEACTIVATE_COLOR_SENSOR_PORT_D)
        }
    }

    fun activateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            "C" -> gattController.writeCharacteristic(ACTIVATE_EXTERNAL_MOTOR_PORT_C)
            "D" -> gattController.writeCharacteristic(ACTIVATE_EXTERNAL_MOTOR_PORT_D)
        }
    }

    fun deactivateExternalMotorSensorNotifications() {
        when (ExternalMotorPort) {
            "C" -> gattController.writeCharacteristic(DEACTIVATE_EXTERNAL_MOTOR_PORT_C)
            "D" -> gattController.writeCharacteristic(DEACTIVATE_EXTERNAL_MOTOR_PORT_D)
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
        gattController.writeCharacteristic(data)
    }

    fun deactivateInternalMotorSensorsNotifications() {
        deactivateInternalMotorSensorNotifications("A")
        deactivateInternalMotorSensorNotifications("B")
    }

    fun deactivateInternalMotorSensorNotifications(motor: String) {
        var data = DEACTIVATE_MOTOR_PORT
        when (motor) {
            "A" -> data[3] = A_PORT_BYTE
            "B" -> data[3] = B_PORT_BYTE
        }
        gattController.writeCharacteristic(data)
    }

    fun activateTiltSensorNotifications() {
        gattController.writeCharacteristic(ACTIVATE_TILT_SENSOR)
    }

    fun deactivateTiltSensorNotifications() {
        gattController.writeCharacteristic(DEACTIVATE_TILT_SENSOR)
    }

    public fun handleNotification(data: ByteArray) {
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
        val intentAction = ACTION_DEVICE_NOTIFICATION
        broadcastUpdate(intentAction, notification)
    }

    private fun runMotor(powerPercentage: Int, timeInMilliseconds: Int, counterclockwise: Boolean, portByte: Byte) {
        val powerByte = when (counterclockwise) {
            true -> (255 - powerPercentage).toByte()
            false -> powerPercentage.toByte()
        }
        val timeBytes = getByteArrayFromInt(timeInMilliseconds, 2)
        val RUN_MOTOR = byteArrayOf(0x0c, 0x00, 0x81.toByte(), portByte, 0x11, 0x09, timeBytes[0], timeBytes[1], powerByte, 0x64, 0x7f, 0x03)
        gattController.writeCharacteristic(RUN_MOTOR)
    }

    fun dumpData() {
        /*for (service in bluetoothGatt!!.services) {
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
        }*/
    }

    public fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val binder = LocalBinder()

    public fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, notification: HubNotification) {
        val intent = Intent(action)
        intent.putExtra(NOTIFICATION_DATA, notification)
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: MoveHubService
            get() = this@MoveHubService
    }

    fun connect() {
        gattController.connect()
    }

    fun disconnect() {
        gattController.disconnect()
    }

    override fun onCreate() {
        gattController.initialize()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        gattController.close()
        return super.onUnbind(intent)
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
