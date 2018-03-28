package com.nateswartz.boostcontroller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.support.v13.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_device_control.*
import android.widget.*

/*
Handle to send data to:
attr handle: 0x000c, end grp handle: 0x000f uuid: 00001623-1212-efde-1623-785feabcd123

handle: 0x000d, char properties: 0x1e, char value handle: 0x000e, uuid: 00001624-1212-efde-1623-785feabcd123
*/
class DeviceControlActivity : Activity(), AdapterView.OnItemSelectedListener {

    private var bluetoothLeService: BluetoothLeService? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var handler: Handler? = null
    private var boostHub: BluetoothDevice? = null
    private var moveHub: MoveHub? = null
    private var connected = false
    private var scanning = false
    private var found = false

    private var colorArray = arrayOf("Off", "Blue", "Pink", "Purple",
            "Light Blue", "Cyan", "Green", "Yellow", "Orange", "Red", "White")

    private val PERMISSION_REQUEST_CODE = 1

    // Device scan callback.
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.e(TAG, result.toString())
            super.onScanResult(callbackType, result)
            boostHub = result.device
            found = true
            scanLeDevice(false)
        }
    }

    // Code to manage Service lifecycle.
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!bluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService!!.connect(boostHub!!.address)
            Log.e(TAG, "Connecting to Boost Hub")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e(TAG, "Service Disconnect")
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
                    enableControls()
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    disableControls()
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> { // Show all the supported services and characteristics on the user interface.
                    if (moveHub == null) {
                        moveHub = MoveHub(bluetoothLeService, bluetoothLeService!!.supportedGattServices!![2].characteristics[0])
                    } else {
                        moveHub!!.update(bluetoothLeService!!, bluetoothLeService!!.supportedGattServices!![2].characteristics[0])
                    }
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
        actionBar!!.setTitle(R.string.title_devices)
        handler = Handler()

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE)

        } else {
            finishSetup()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finishSetup()
                    return
                }
            }
            else -> {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (connected) {
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            if (!found) {
                menu.findItem(R.id.menu_scan).isVisible = true
            }
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_connect).isEnabled = !scanning
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                scanLeDevice(true)
                scanning = true
            }
            R.id.menu_connect -> {
                if (scanning) {
                    bluetoothScanner!!.stopScan(leScanCallback)
                    scanning = false
                }
                Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
                val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
                bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                return true
            }
            R.id.menu_disconnect -> {
                bluetoothLeService!!.disconnect()
                connected = false
                unbindService(serviceConnection)
                bluetoothLeService = null
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler!!.postDelayed({
                scanning = false
                found = false
                bluetoothScanner!!.stopScan(leScanCallback)
                invalidateOptionsMenu()
            }, SCAN_PERIOD)

            Log.e(TAG, "Scanning")
            scanning = true
            bluetoothScanner!!.startScan(
                    listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BoostUUID)).build()),
                    ScanSettings.Builder().build(),
                    leScanCallback)
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()

        } else {
            Log.e(TAG, "Stop Scanning")
            scanning = false
            bluetoothScanner!!.stopScan(leScanCallback)
            Toast.makeText(this, "Scanning Stopped", Toast.LENGTH_SHORT).show()
        }
        invalidateOptionsMenu()
    }

    private fun finishSetup() {
        Log.e(TAG, "FinishSetup")
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter!!.bluetoothLeScanner

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        scanLeDevice(true)

        val adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, colorArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_led_colors.adapter = adapter
        spinner_led_colors.onItemSelectedListener = this
        // Set Spinner to Blue to start (since that's the Hub default)
        spinner_led_colors.setSelection(1)

        disableControls()

        button_enable_button.setOnClickListener{
            moveHub!!.activateButton()
        }
        button_enable_color_sensor.setOnClickListener{
            moveHub!!.activateColorSensor()
        }
        button_enable_imotor.setOnClickListener{
            moveHub!!.activateExternalMotorSensor()
        }
        button_enable_motors.setOnClickListener {
            moveHub!!.activateInternalMotorSensors()
        }

        button_imotor_run.setOnClickListener {
            moveHub!!.runExternalMotor(25, 400, false)
        }
        button_imotor_reverse.setOnClickListener {
            moveHub!!.runExternalMotor(25, 400, true)
        }

        button_run_internal_motors.setOnClickListener {
            moveHub!!.runInternalMotors(25, 400, false)
        }
        button_reverse_internal_motors.setOnClickListener {
            moveHub!!.runInternalMotors(25, 400, true)
        }

        button_run_motor_a.setOnClickListener{
            moveHub!!.runInternalMotor(25, 400, false, "A")
        }

        button_run_motor_b.setOnClickListener{
            moveHub!!.runInternalMotor(25, 400, false, "B")
        }

        button_spin.setOnClickListener {
            moveHub!!.runInternalMotorsInOpposition(20, 300)
        }

        button_dump_data.setOnClickListener {
            bluetoothLeService!!.dumpData()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = parent!!.getItemAtPosition(position).toString()
        val color = getLedColorFromName(item)
        moveHub?.setLEDColor(color)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun enableControls() {
        setControlsState(true)
    }

    private fun disableControls() {
        setControlsState(false)
    }

    private fun setControlsState(enabled : Boolean) {
        button_enable_imotor.isEnabled = enabled
        button_enable_color_sensor.isEnabled = enabled
        button_enable_button.isEnabled = enabled
        button_enable_motors.isEnabled = enabled
        button_imotor_run.isEnabled = enabled
        button_imotor_reverse.isEnabled = enabled
        button_dump_data.isEnabled = enabled
        button_run_internal_motors.isEnabled = enabled
        button_reverse_internal_motors.isEnabled = enabled
        button_spin.isEnabled = enabled
        button_run_motor_a.isEnabled = enabled
        button_run_motor_b.isEnabled = enabled
        spinner_led_colors.isEnabled = enabled
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE)
        } else {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!bluetoothAdapter!!.isEnabled) {
                if (!bluetoothAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
            }

            if (!found) {
                scanLeDevice(true)
            }
        }

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(boostHub!!.address)
            Log.d(TAG, "Connect request result=$result")
        }

    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (scanning) {
                scanLeDevice(false)
                scanning = false
            } else {
                if (connected) {
                    bluetoothLeService!!.disconnect()
                    connected = false
                    unbindService(serviceConnection)
                    bluetoothLeService = null
                }
            }
        }
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        bluetoothLeService = null
    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName

        const val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        const val SCAN_PERIOD: Long = 10000

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
