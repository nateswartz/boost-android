package com.nateswartz.boostcontroller

import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_device_control.*
import java.util.*

/*
Handle to send data to:
attr handle: 0x000c, end grp handle: 0x000f uuid: 00001623-1212-efde-1623-785feabcd123

handle: 0x000d, char properties: 0x1e, char value handle: 0x000e, uuid: 00001624-1212-efde-1623-785feabcd123
*/

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : Activity() {

    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private var bluetoothLeService: BluetoothLeService? = null
    private var moveHub: MoveHub? = null
    private var gattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var connected = false

    private val listName = "NAME"
    private val listUUID = "UUID"



    // Code to manage Service lifecycle.
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!bluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService!!.connect(deviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> { // Show all the supported services and characteristics on the user interface.
                    moveHub = MoveHub(bluetoothLeService, bluetoothLeService!!.supportedGattServices!![2].characteristics[0])
                    moveHub!!.enableNotifications()
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    moveHub!!.handleNotification(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        val intent = intent
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        actionBar!!.title = deviceName
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        button_purple.setOnClickListener {
            moveHub!!.setLEDColor(LEDColor.PURPLE)
        }
        button_white.setOnClickListener {
            moveHub!!.setLEDColor(LEDColor.WHITE)
        }
        button_yellow.setOnClickListener {
            moveHub!!.setLEDColor(LEDColor.YELLOW)
        }
        button_pink.setOnClickListener {
            moveHub!!.setLEDColor(LEDColor.PINK)
        }

        button_enable_button.setOnClickListener{
            moveHub!!.activateButton()
        }
        button_enable_color_sensor.setOnClickListener{
            moveHub!!.activateColorSensor()
        }
        button_enable_imotor.setOnClickListener{
            moveHub!!.activateExternalMotorSensor()
        }

        button_imotor_run.setOnClickListener{
            moveHub!!.runExternalMotor(25, 400, false)
        }

        button_imotor_reverse.setOnClickListener {
            moveHub!!.runExternalMotor(25, 400, true)
        }

        button_dump_data.setOnClickListener {
            bluetoothLeService!!.dumpData()
        }
    }



    private fun randomColor() {
        var color : LEDColor? = null
        val number = Random().nextInt(3)
        when (number) {
            0 -> color = LEDColor.PINK
            1 -> color = LEDColor.PURPLE
            2 -> color = LEDColor.WHITE
        }
        moveHub!!.setLEDColor(color!!)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(deviceAddress)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        bluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (connected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                bluetoothLeService!!.connect(deviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                bluetoothLeService!!.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName

        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}
