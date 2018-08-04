package com.nateswartz.boostcontroller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v13.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_device_control.*
import com.orbotix.DualStackDiscoveryAgent
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.orbotix.ConvenienceRobot
import com.orbotix.common.DiscoveryException
import com.orbotix.common.Robot
import com.orbotix.common.RobotChangedStateListener

/*
Handle to send data to:
attr handle: 0x000c, end grp handle: 0x000f uuid: 00001623-1212-efde-1623-785feabcd123

handle: 0x000d, char properties: 0x1e, char value handle: 0x000e, uuid: 00001624-1212-efde-1623-785feabcd123
*/
class DeviceControlActivity : Activity(), AdapterView.OnItemSelectedListener, RobotChangedStateListener {

    // Lifx
    private val apiToken = ""
    private val lightID = ""
    private var connectBoostToLifx = false

    // Sphero
    private val mDiscoveryAgent = DualStackDiscoveryAgent()
    private var mRobot: ConvenienceRobot? = null
    private var click = 0

    private var bluetoothDeviceService: BluetoothDeviceService? = null
    private var connectedBoost = false
    private var connectingBoost = false

    private var connectedLpf2 = false
    private var connectingLpf2 = false

    private var colorArray = arrayOf("Off", "Blue", "Pink", "Purple",
            "Light Blue", "Cyan", "Green", "Yellow", "Orange", "Red", "White")

    private var motorTypes = arrayOf("A", "B", "A+B", "External")

    private val PERMISSION_REQUEST_CODE = 1

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothDeviceService = (service as BluetoothDeviceService.LocalBinder).service
            finishSetup()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "Service Disconnect")
            bluetoothDeviceService = null
        }
    }

    private val moveHubUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDeviceService.ACTION_BOOST_CONNECTED -> {
                    connectedBoost = true
                    connectingBoost = false
                    text_boost_connected.visibility = View.VISIBLE
                    bluetoothDeviceService!!.moveHubController.enableNotifications()
                    enableControls()
                    invalidateOptionsMenu()
                }
                BluetoothDeviceService.ACTION_BOOST_DISCONNECTED -> {
                    connectedBoost = false
                    connectingBoost = false
                    text_boost_connected.visibility = View.INVISIBLE
                    disableControls()
                    invalidateOptionsMenu()
                }
                BluetoothDeviceService.ACTION_LPF2_CONNECTED -> {
                    connectedLpf2 = true
                    connectingLpf2 = false
                    text_lpf2_connected.visibility = View.VISIBLE
                }
                BluetoothDeviceService.ACTION_LPF2_DISCONNECTED -> {
                    connectedLpf2 = false
                    connectingLpf2 = false
                    text_lpf2_connected.visibility = View.INVISIBLE
                }
                BluetoothDeviceService.ACTION_DEVICE_CONNECTION_FAILED -> {
                    connectingBoost = false
                    connectingLpf2 = false
                    invalidateOptionsMenu()
                    Toast.makeText(this@DeviceControlActivity, "Connection Failed!", Toast.LENGTH_SHORT).show()
                }
                BluetoothDeviceService.ACTION_DEVICE_NOTIFICATION -> {
                    val notification = intent.getParcelableExtra<HubNotification>(BluetoothDeviceService.NOTIFICATION_DATA)
                    if (switch_sync_colors.isChecked && notification is ColorSensorNotification) {
                        bluetoothDeviceService!!.moveHubController.setLEDColor(getLedColorFromName(notification.color.string))
                    }
                    if (notification is ButtonNotification && mRobot != null) {
                        changeSpheroColor()
                    }
                    if (switch_connect_boost_lifx.isChecked && notification is ButtonNotification) {
                        toggleLifx()
                    }
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDiscoveryAgent.addRobotStateListener(this)

        setContentView(R.layout.activity_device_control)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE)

        } else {
            val moveHubServiceIntent = Intent(this, BluetoothDeviceService::class.java)
            bindService(moveHubServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val moveHubServiceIntent = Intent(this, BluetoothDeviceService::class.java)
                    bindService(moveHubServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                    return
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
    }

    // Sphero
    private fun startDiscovery() {
        //If the DiscoveryAgent is not already looking for robots, start discovery.
        if( !mDiscoveryAgent.isDiscovering ) {
            try {
                Log.d("Sphero", "Looking for Sphero")
                mDiscoveryAgent.startDiscovery(applicationContext)
            } catch (e: DiscoveryException) {
                Log.e("Sphero", "DiscoveryException: " + e.message)
            }
        }
    }

    // Sphero
    override fun handleRobotChangedState(robot: Robot, type: RobotChangedStateListener.RobotChangedStateNotificationType) {
        Log.d("Sphero", "handleRobotChangedState $type")
        when (type) {
            RobotChangedStateListener.RobotChangedStateNotificationType.Connected -> {
                mRobot = ConvenienceRobot(robot)
                button_sphero_color.isEnabled = true
                button_sphero_connect.isEnabled = false
            }
            RobotChangedStateListener.RobotChangedStateNotificationType.Online -> {
                mRobot = ConvenienceRobot(robot)
                button_sphero_color.isEnabled = true
                button_sphero_connect.isEnabled = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (connectedBoost || connectedLpf2) {
            menu.findItem(R.id.menu_disconnect).isVisible = true
            menu.findItem(R.id.menu_connect).isVisible = !(connectedBoost && connectedLpf2)
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
            if (connectingBoost || connectingLpf2) {
                menu.findItem(R.id.menu_connect).isEnabled = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                Log.d(TAG, "Connecting...")
                Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
                connectingBoost = !connectedBoost
                connectingLpf2 = !connectedLpf2
                bluetoothDeviceService!!.connect()
                return true
            }
            R.id.menu_disconnect -> {
                Log.d(TAG, "Disconnecting...")
                bluetoothDeviceService!!.disconnect()
                connectedBoost = false
                connectedLpf2 = false
                text_boost_connected.visibility = View.INVISIBLE
                text_lpf2_connected.visibility = View.INVISIBLE
                invalidateOptionsMenu()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult Enter")
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun finishSetup() {
        Log.d(TAG, "FinishSetup")
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        connectingBoost = true
        connectingLpf2 = true
        bluetoothDeviceService!!.connect()

        val colorAdapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, colorArray)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_led_colors.adapter = colorAdapter
        spinner_led_colors.onItemSelectedListener = this
        // Set Spinner to Blue to start (since that's the Hub default)
        spinner_led_colors.setSelection(1)

        val motorAdapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, motorTypes)
        motorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_motor_types.adapter = motorAdapter
        spinner_motor_types.onItemSelectedListener = this

        disableControls()

        switch_color_sensor.setOnClickListener{
            if (switch_color_sensor.isChecked) {
                bluetoothDeviceService!!.moveHubController.activateColorSensorNotifications()
            } else {
                bluetoothDeviceService!!.moveHubController.deactivateColorSensorNotifications()
            }
        }

        switch_tilt_sensor.setOnClickListener {
            if (switch_tilt_sensor.isChecked) {
                bluetoothDeviceService!!.moveHubController.activateTiltSensorNotifications()
            } else {
                bluetoothDeviceService!!.moveHubController.deactivateTiltSensorNotifications()
            }
        }

        switch_internal_motors.setOnClickListener {
            if (switch_internal_motors.isChecked) {
                bluetoothDeviceService!!.moveHubController.activateInternalMotorSensorsNotifications()
            } else {
                bluetoothDeviceService!!.moveHubController.deactivateInternalMotorSensorsNotifications()
            }
        }

        switch_external_motor.setOnClickListener {
            if (switch_external_motor.isChecked) {
                bluetoothDeviceService!!.moveHubController.activateExternalMotorSensorNotifications()
            } else {
                bluetoothDeviceService!!.moveHubController.deactivateExternalMotorSensorNotifications()
            }
        }

        switch_button.setOnClickListener {
            if (switch_button.isChecked) {
                bluetoothDeviceService!!.moveHubController.activateButtonNotifications()
            } else {
                // Currently not working
                bluetoothDeviceService!!.moveHubController.deactivateButtonNotifications()
            }
        }

        button_spin.setOnClickListener {
            bluetoothDeviceService!!.moveHubController.runInternalMotorsInOpposition(20, 300)
        }

        button_dump_data.setOnClickListener {
            //bluetoothDeviceService!!.dumpData()
        }

        button_var_run_motor.setOnClickListener {
            val power = input_power.text.toString()
            val time = input_time.text.toString()
            val motor = spinner_motor_types.selectedItem.toString()
            val counterclockwise = switch_counter_clockwise.isChecked
            if (power != "" && time != "") {
                when (motor) {
                    "A" -> bluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, "A")
                    "B" -> bluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, "B")
                    "A+B" -> bluetoothDeviceService!!.moveHubController.runInternalMotors(power.toInt(), time.toInt(), counterclockwise)
                    "External" -> bluetoothDeviceService!!.moveHubController.runExternalMotor(power.toInt(), time.toInt(), counterclockwise)
                }
            }
        }

        // Sphero
        button_sphero_connect.setOnClickListener {
            Toast.makeText(this, "Connecting to Sphero...", Toast.LENGTH_SHORT).show()
            startDiscovery()
        }

        button_sphero_color.setOnClickListener {
            changeSpheroColor()
        }

        // Lifx
        switch_sync_colors.setOnClickListener {
            connectBoostToLifx = switch_sync_colors.isChecked

        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (connectedBoost) {
            when (parent) {
                spinner_led_colors -> {
                    val item = parent!!.getItemAtPosition(position).toString()
                    val color = getLedColorFromName(item)
                    bluetoothDeviceService?.moveHubController?.setLEDColor(color)
                }
            }
        }
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
        button_dump_data.isEnabled = enabled
        button_spin.isEnabled = enabled
        spinner_led_colors.isEnabled = enabled
        spinner_motor_types.isEnabled = enabled
        text_power.isEnabled = enabled
        text_time.isEnabled = enabled
        input_power.isEnabled = enabled
        input_time.isEnabled = enabled
        button_var_run_motor.isEnabled = enabled
        switch_counter_clockwise.isEnabled = enabled
        switch_sync_colors.isEnabled = enabled
        switch_color_sensor.isEnabled = enabled
        switch_tilt_sensor.isEnabled = enabled
        switch_button.isEnabled = enabled
        switch_external_motor.isEnabled = enabled
        switch_internal_motors.isEnabled = enabled
    }

    private fun changeSpheroColor() {
        click++
        if (click % 2 == 0) {
            mRobot!!.setLed(0.0f, 1.0f, 0.0f)
        } else {
            mRobot!!.setLed(1.0f, 0.0f, 0.0f)
        }
    }

    private fun toggleLifx() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.lifx.com/v1/lights/id:$lightID/toggle"
        val headers = HashMap<String, String>()
        headers["Authorization"] = "Bearer $apiToken"

        // Request a string response from the provided URL.
        val postRequest = object : StringRequest(Request.Method.POST, url,
                Response.Listener<String> { response ->
                    Log.d("Volley", "Success: $response")
                },
                Response.ErrorListener {
                    // error
                    Log.e("Volley", "Error")
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return headers
            }
        }

        // Add the request to the RequestQueue.
        queue.add(postRequest)
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
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        registerReceiver(moveHubUpdateReceiver, makeMoveHubUpdateIntentFilter())
        if (bluetoothDeviceService != null) {
            val result = bluetoothDeviceService!!.connect()
            Log.d(TAG, "Connect request result=$result")
        }

    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (connectingBoost || connectedBoost || connectingLpf2 || connectedLpf2) {
                bluetoothDeviceService!!.disconnect()
                connectedBoost = false
                connectingBoost = false
                connectedLpf2 = false
                connectingLpf2 = false
                text_boost_connected.visibility = View.INVISIBLE
                text_lpf2_connected.visibility = View.INVISIBLE
            }
        }
        unregisterReceiver(moveHubUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        bluetoothDeviceService = null

        // Sphero
        // If the DiscoveryAgent is in discovery mode, stop it.
        if (mDiscoveryAgent.isDiscovering) {
            mDiscoveryAgent.stopDiscovery()
        }

        // If a robot is connected to the device, disconnect it
        mRobot?.disconnect()
        mRobot = null
        button_sphero_color.isEnabled = false
    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName

        const val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        const val SCAN_PERIOD: Long = 10000

        private fun makeMoveHubUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothDeviceService.ACTION_BOOST_CONNECTED)
            intentFilter.addAction(BluetoothDeviceService.ACTION_BOOST_DISCONNECTED)
            intentFilter.addAction(BluetoothDeviceService.ACTION_LPF2_CONNECTED)
            intentFilter.addAction(BluetoothDeviceService.ACTION_LPF2_DISCONNECTED)
            intentFilter.addAction(BluetoothDeviceService.ACTION_DEVICE_CONNECTION_FAILED)
            intentFilter.addAction(BluetoothDeviceService.ACTION_DEVICE_NOTIFICATION)
            return intentFilter
        }
    }
}
