package com.nateswartz.boostcontroller

import android.content.Context
import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_actions.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

private var colorArray = arrayOf("Off", "Blue", "Pink", "Purple",
        "Light Blue", "Cyan", "Green", "Yellow", "Orange", "Red", "White")
private var motorTypes = arrayOf("A", "B", "A+B", "External")

class ActionsFragment : Fragment(), AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var BluetoothDeviceService: BluetoothDeviceService? = null
    //private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
            BluetoothDeviceService!!.moveHubController.runInternalMotorsInOpposition(20, 300)
        }

        button_var_run_motor.setOnClickListener {
            val power = input_power.text.toString()
            val time = input_time.text.toString()
            val motor = spinner_motor_types.selectedItem.toString()
            val counterclockwise = switch_counter_clockwise.isChecked
            if (power != "" && time != "") {
                when (motor) {
                    "A" -> BluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, Port.A)
                    "B" -> BluetoothDeviceService!!.moveHubController.runInternalMotor(power.toInt(), time.toInt(), counterclockwise, Port.B)
                    "A+B" -> BluetoothDeviceService!!.moveHubController.runInternalMotors(power.toInt(), time.toInt(), counterclockwise)
                    "External" -> BluetoothDeviceService!!.moveHubController.runExternalMotor(power.toInt(), time.toInt(), counterclockwise)
                }
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (BluetoothDeviceService?.moveHubController != null) {
            when (parent) {
                spinner_led_colors -> {
                    val item = parent!!.getItemAtPosition(position).toString()
                    val color = getLedColorFromName(item)
                    BluetoothDeviceService?.moveHubController?.setLEDColor(color)
                }
            }
        }
    }

    // TODO: Find a better way to access the BluetoothDeviceService
    fun setBluetoothDeviceService(bluetoothDeviceService: BluetoothDeviceService) {
        BluetoothDeviceService = bluetoothDeviceService
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
/*        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }*/
    }

    override fun onDetach() {
        super.onDetach()
        //listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
/*    interface OnFragmentInteractionListener {
        //fun onChangeNotification(type: NotificationType, enabled: Boolean)
    }*/

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificationSettings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                NotificationSettingsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
