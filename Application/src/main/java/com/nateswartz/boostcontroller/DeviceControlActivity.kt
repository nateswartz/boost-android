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
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_device_control.*
import android.widget.*
import com.orbotix.ConvenienceRobot
import com.orbotix.common.RobotChangedStateListener


class DeviceControlActivity : Activity(), SpheroServiceListener, NotificationSettingsFragment.OnFragmentInteractionListener {
    private var notificationSettingsFragment: NotificationSettingsFragment? = null
    private var actionsFragment: ActionsFragment? = null

    // Lifx
    private var lifxController: LifxController? = null

    // Sphero
    private var isSpheroServiceBound = false
    private var sphero: ConvenienceRobot? = null

    private var legoBluetoothDeviceService: LegoBluetoothDeviceService? = null
    private var connectedBoost = false
    private var connectingBoost = false

    private var connectedLpf2 = false
    private var connectingLpf2 = false

    private var notificationListeners = mutableMapOf<String, HubNotificationListener>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            legoBluetoothDeviceService = (service as LegoBluetoothDeviceService.LocalBinder).service
            notificationSettingsFragment!!.setLegoBluetoothDeviceService(legoBluetoothDeviceService!!)
            actionsFragment!!.setLegoBluetoothDeviceService(legoBluetoothDeviceService!!)
            finishSetup()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "Service Disconnect")
            legoBluetoothDeviceService = null
            actionsFragment!!.setLegoBluetoothDeviceService(null)
        }
    }

    private val moveHubUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                LegoBluetoothDeviceService.ACTION_BOOST_CONNECTED -> {
                    connectedBoost = true
                    connectingBoost = false
                    notificationSettingsFragment!!.boostConnectionChanged(connectedBoost)
                    actionsFragment!!.boostConnectionChanged(connectedBoost)
                    legoBluetoothDeviceService!!.moveHubController.enableNotifications()
                    enableControls()
                    invalidateOptionsMenu()
                }
                LegoBluetoothDeviceService.ACTION_BOOST_DISCONNECTED -> {
                    connectedBoost = false
                    connectingBoost = false
                    notificationSettingsFragment!!.boostConnectionChanged(connectedBoost)
                    actionsFragment!!.boostConnectionChanged(connectedBoost)
                    disableControls()
                    invalidateOptionsMenu()
                }
                LegoBluetoothDeviceService.ACTION_LPF2_CONNECTED -> {
                    connectedLpf2 = true
                    connectingLpf2 = false
                    notificationSettingsFragment!!.lpf2ConnectionChanged(connectedLpf2)
                }
                LegoBluetoothDeviceService.ACTION_LPF2_DISCONNECTED -> {
                    connectedLpf2 = false
                    connectingLpf2 = false
                    notificationSettingsFragment!!.lpf2ConnectionChanged(connectedLpf2)
                }
                LegoBluetoothDeviceService.ACTION_DEVICE_CONNECTION_FAILED -> {
                    connectingBoost = false
                    connectingLpf2 = false
                    invalidateOptionsMenu()
                    Toast.makeText(this@DeviceControlActivity, "Connection Failed!", Toast.LENGTH_SHORT).show()
                }
                LegoBluetoothDeviceService.ACTION_DEVICE_NOTIFICATION -> {
                    val notification = intent.getParcelableExtra<HubNotification>(LegoBluetoothDeviceService.NOTIFICATION_DATA)

                    if (notification is PortInfoNotification) {
                        when (notification.sensor) {
                            Sensor.DISTANCE_COLOR -> notificationSettingsFragment!!.colorSensorConnectionChanged(true)
                            Sensor.EXTERNAL_MOTOR -> notificationSettingsFragment!!.externalMotorConnectionChanged(true)
                        }
                    }
                    if (notification is PortDisconnectedNotification) {
                        when (notification.sensor) {
                            Sensor.EXTERNAL_MOTOR -> notificationSettingsFragment!!.externalMotorConnectionChanged(false)
                            Sensor.DISTANCE_COLOR -> notificationSettingsFragment!!.colorSensorConnectionChanged(false)
                        }
                    }
                    for (listener in notificationListeners) {
                        listener.value.execute(notification)
                    }
                }
            }
        }
    }

    private val spheroServiceConnection = object : ServiceConnection {
        private var boundService: SpheroProviderService? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG,"onServiceConnected")
            boundService = (service as SpheroProviderService.RobotBinder).service
            isSpheroServiceBound = true
            boundService?.addListener(this@DeviceControlActivity)
            if (boundService?.hasActiveSphero() == true) {
                handleSpheroChange(boundService!!.getSphero(), RobotChangedStateListener.RobotChangedStateNotificationType.Online)
            } else {
                val toast = Toast.makeText(this@DeviceControlActivity, "Discovering...",
                        Toast.LENGTH_LONG)
                toast.setGravity(Gravity.BOTTOM, 0, 10)
                toast.show()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG,"onServiceDisconnected")
            boundService = null
            isSpheroServiceBound = false
            boundService?.removeListener(this@DeviceControlActivity)
        }
    }

    override fun handleSpheroChange(robot: ConvenienceRobot, type: RobotChangedStateListener.RobotChangedStateNotificationType) {
        when (type) {
            RobotChangedStateListener.RobotChangedStateNotificationType.Online -> {
                Log.d(TAG, "handleRobotOnline")
                val toast = Toast.makeText(this@DeviceControlActivity, "Connected!",
                        Toast.LENGTH_LONG)
                toast.setGravity(Gravity.BOTTOM, 0, 10)
                toast.show()
                sphero = robot
                switch_sphero_color_button.isEnabled = true
                switch_sphero_color_tilt.isEnabled = true
                button_sphero_connect.text = "Disconnect Sphero"
                button_sphero_connect.isEnabled = true
            }
            RobotChangedStateListener.RobotChangedStateNotificationType.Offline -> {
                Log.d(TAG, "handleRobotDisconnected")
                button_sphero_connect.text = "Connect Sphero"
                button_sphero_connect.isEnabled = true
                sphero = null
            }
            RobotChangedStateListener.RobotChangedStateNotificationType.Connecting -> {
                Log.d(TAG, "handleRobotConnecting")
                val toast = Toast.makeText(this@DeviceControlActivity, "Connecting..",
                        Toast.LENGTH_LONG)
                toast.setGravity(Gravity.BOTTOM, 0, 10)
                toast.show()
            }
            else -> {
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_device_control)

        lifxController = LifxController(this)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE)
        } else {
            val moveHubServiceIntent = Intent(this, LegoBluetoothDeviceService::class.java)
            bindService(moveHubServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val moveHubServiceIntent = Intent(this, LegoBluetoothDeviceService::class.java)
                    bindService(moveHubServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

                    return
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        notificationSettingsFragment = fragmentManager.findFragmentById(R.id.notifications_fragement) as NotificationSettingsFragment
        actionsFragment = fragmentManager.findFragmentById(R.id.actions_fragment) as ActionsFragment
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .hide(actionsFragment)
                .commit()
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
                legoBluetoothDeviceService!!.connect()
                return true
            }
            R.id.menu_disconnect -> {
                Log.d(TAG, "Disconnecting...")
                legoBluetoothDeviceService!!.disconnect()
                connectedBoost = false
                connectedLpf2 = false
                notificationSettingsFragment!!.boostConnectionChanged(connectedBoost)
                notificationSettingsFragment!!.lpf2ConnectionChanged(connectedLpf2)
                actionsFragment!!.boostConnectionChanged(connectedBoost)
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
        legoBluetoothDeviceService!!.connect()

        disableControls()
        switch_sphero_color_button.isEnabled = false
        switch_sphero_color_tilt.isEnabled = false

        // Sphero
        button_sphero_connect.setOnClickListener {
            if (isSpheroServiceBound) {
                Toast.makeText(this, "Disconnecting Sphero...", Toast.LENGTH_SHORT).show()
                unbindService(spheroServiceConnection)
            } else {
                Toast.makeText(this, "Connecting to Sphero...", Toast.LENGTH_SHORT).show()
                val spheroServiceIntent = Intent(this, SpheroProviderService::class.java)
                bindService(spheroServiceIntent, spheroServiceConnection, Context.BIND_AUTO_CREATE)
            }
            button_sphero_connect.isEnabled = false
        }

        switch_sphero_color_button.setOnClickListener {
            if (switch_sphero_color_button.isChecked) {
                notificationListeners["button_sphero"] = ChangeSpheroColorOnButton(sphero!!)
            } else {
                notificationListeners.remove("button_sphero")
            }
        }

        switch_sphero_color_tilt.setOnClickListener {
            if (switch_sphero_color_tilt.isChecked) {
                notificationListeners["tilt_sphero"] = ChangeSpheroColorOnTilt(sphero!!)
            } else {
                notificationListeners.remove("tilt_sphero")
            }
        }

        switch_sync_colors.setOnClickListener {
            if (switch_sync_colors.isChecked) {
                notificationListeners["sync_colors"] = ChangeLEDOnColorSensor(legoBluetoothDeviceService!!)
            } else {
                notificationListeners.remove("sync_colors")
            }
        }

        switch_button_change_light.setOnClickListener {
            if (switch_button_change_light.isChecked) {
                notificationListeners["button_change_light"] = ChangeLEDOnButtonClick(legoBluetoothDeviceService!!)
            } else {
                notificationListeners.remove("button_change_light")
            }
        }

        switch_button_change_motor.setOnClickListener {
            if (switch_button_change_motor.isChecked) {
                notificationListeners["button_change_motor"] = RunMotorOnButtonClick(legoBluetoothDeviceService!!)
            } else {
                notificationListeners.remove("button_change_motor")
            }
        }

        switch_roller_coaster.setOnClickListener {
            if (switch_roller_coaster.isChecked) {
                //val time = if (input_time.text.toString() == "") "1000" else input_time.text.toString()
                //notificationListeners["roller_coaster"] = RollerCoaster(time, switch_counter_clockwise.isChecked, legoBluetoothDeviceService!!)
                notificationListeners["roller_coaster"] = RollerCoaster("2000", true, legoBluetoothDeviceService!!)
            } else {
                notificationListeners.remove("roller_coaster")
            }
        }

        switch_motor_button_lifx.setOnClickListener {
            if (switch_motor_button_lifx.isChecked) {
                notificationListeners["motor_button_led_lifx"] = ChangeLifxLEDOnMotorButton(legoBluetoothDeviceService!!, lifxController!!)
            } else {
                notificationListeners.remove("motor_button_led_lifx")
            }
        }

        textview_notifications.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
                                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            if (notificationSettingsFragment!!.isHidden){
                transaction.show(notificationSettingsFragment)
            } else {
                transaction.hide(notificationSettingsFragment)
            }
            transaction.commit()
        }

        textview_actions.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            if (actionsFragment!!.isHidden){
                transaction.show(actionsFragment)
            } else {
                transaction.hide(actionsFragment)
            }
            transaction.commit()
        }
    }

    private fun enableControls() {
        setControlsState(true)
    }

    private fun disableControls() {
        setControlsState(false)
    }

    private fun setControlsState(enabled : Boolean) {
        switch_sync_colors.isEnabled = enabled
        switch_button_change_motor.isEnabled = enabled
        switch_button_change_light.isEnabled = enabled
        switch_motor_button_lifx.isEnabled = enabled
        switch_roller_coaster.isEnabled = enabled
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
        if (legoBluetoothDeviceService != null) {
            val result = legoBluetoothDeviceService!!.connect()
            Log.d(TAG, "Connect request result=$result")
        }

    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (connectingBoost || connectedBoost || connectingLpf2 || connectedLpf2) {
                legoBluetoothDeviceService!!.disconnect()
                connectedBoost = false
                connectingBoost = false
                connectedLpf2 = false
                connectingLpf2 = false
                notificationSettingsFragment!!.boostConnectionChanged(connectedBoost)
                actionsFragment!!.boostConnectionChanged(connectedBoost)
                notificationSettingsFragment!!.lpf2ConnectionChanged(connectedLpf2)
            }
        }
        unregisterReceiver(moveHubUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        if (isSpheroServiceBound) {
            unbindService(spheroServiceConnection)
        }
        legoBluetoothDeviceService = null

        switch_sphero_color_button.isEnabled = false
    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName

        const val PERMISSION_REQUEST_CODE = 1

        const val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        const val SCAN_PERIOD: Long = 10000

        private fun makeMoveHubUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_BOOST_CONNECTED)
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_BOOST_DISCONNECTED)
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_LPF2_CONNECTED)
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_LPF2_DISCONNECTED)
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_DEVICE_CONNECTION_FAILED)
            intentFilter.addAction(LegoBluetoothDeviceService.ACTION_DEVICE_NOTIFICATION)
            return intentFilter
        }
    }
}
