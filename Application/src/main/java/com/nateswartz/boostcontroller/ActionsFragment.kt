package com.nateswartz.boostcontroller

import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_actions.*

class ActionsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var legoBluetoothDeviceService: LegoBluetoothDeviceService? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_actions, container, false)
    }

    override fun onStart() {
        super.onStart()
        val colorAdapter = ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, colorArray)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_led_colors.adapter = colorAdapter
        spinner_led_colors.onItemSelectedListener = this
        // Set Spinner to Blue to start (since that's the Hub default)
        spinner_led_colors.setSelection(1)

        val motorAdapter = ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, motorTypes)
        motorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_motor_types.adapter = motorAdapter
        spinner_motor_types.onItemSelectedListener = this

        button_spin.setOnClickListener {
            legoBluetoothDeviceService!!.moveHubController.runInternalMotorsInOpposition(20, 300)
        }

        button_var_run_motor.setOnClickListener {
            val power = input_power.text.toString()
            val time = input_time.text.toString()
            val motor = spinner_motor_types.selectedItem.toString()
            val counterclockwise = switch_counter_clockwise.isChecked
            if (power != "" && time != "") {
                when (motor) {
                    "A" -> legoBluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, Port.A)
                    "B" -> legoBluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, Port.B)
                    "A+B" -> legoBluetoothDeviceService!!.moveHubController.runInternalMotors(power.toInt(), time.toInt(), counterclockwise)
                    "External" -> legoBluetoothDeviceService!!.moveHubController.runExternalMotor(power.toInt(), time.toInt(), counterclockwise)
                }
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (legoBluetoothDeviceService?.moveHubController != null) {
            when (parent) {
                spinner_led_colors -> {
                    val item = parent!!.getItemAtPosition(position).toString()
                    val color = getLedColorFromName(item)
                    legoBluetoothDeviceService?.moveHubController?.setLEDColor(color)
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    // TODO: Find a better way to access the legoBluetoothDeviceService
    fun setLegoBluetoothDeviceService(legoBluetoothDeviceService: LegoBluetoothDeviceService?) {
        this.legoBluetoothDeviceService = legoBluetoothDeviceService
    }

    fun boostConnectionChanged(isConnected: Boolean) {
        button_spin.isEnabled = isConnected
        spinner_led_colors.isEnabled = isConnected
        spinner_motor_types.isEnabled = isConnected
        text_power.isEnabled = isConnected
        text_time.isEnabled = isConnected
        input_power.isEnabled = isConnected
        input_time.isEnabled = isConnected
        button_var_run_motor.isEnabled = isConnected
        switch_counter_clockwise.isEnabled = isConnected
    }

    companion object {
        private var colorArray = arrayOf("Off", "Blue", "Pink", "Purple",
                "Light Blue", "Cyan", "Green", "Yellow", "Orange", "Red", "White")
        private var motorTypes = arrayOf("A", "B", "A+B", "External")

    }
}
