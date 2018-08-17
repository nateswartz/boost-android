package com.nateswartz.boostcontroller

import android.content.Context
import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_notification_settings.*


enum class NotificationType {
    BUTTON, COLOR_SENSOR, TILT_SENSOR, INTERNAL_MOTORS, EXTERNAL_MOTOR
}

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [NotificationSettings.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [NotificationSettings.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class NotificationSettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var BluetoothDeviceService: BluetoothDeviceService? = null
    private var listener: OnFragmentInteractionListener? = null

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
        return inflater.inflate(R.layout.fragment_notification_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        checkbox_all_notifications.setOnClickListener {
            if (checkbox_button.isChecked != checkbox_all_notifications.isChecked) {
                checkbox_button.performClick()
            }
            if (checkbox_internal_motors.isChecked != checkbox_all_notifications.isChecked) {
                checkbox_internal_motors.performClick()
            }
            if (checkbox_external_motor.isChecked != checkbox_all_notifications.isChecked) {
                checkbox_external_motor.performClick()
            }
            if (checkbox_color_sensor.isChecked != checkbox_all_notifications.isChecked) {
                checkbox_color_sensor.performClick()
            }
            if (checkbox_tilt_sensor.isChecked != checkbox_all_notifications.isChecked) {
                checkbox_tilt_sensor.performClick()
            }
        }
        checkbox_button.setOnClickListener {
            if (checkbox_button.isChecked) {
                BluetoothDeviceService!!.moveHubController.activateButtonNotifications()
            } else {
                // Currently not working
                BluetoothDeviceService!!.moveHubController.deactivateButtonNotifications()
            }
        }
        checkbox_color_sensor.setOnClickListener {
            if (checkbox_color_sensor.isChecked) {
                BluetoothDeviceService!!.moveHubController.activateColorSensorNotifications()
            } else {
                BluetoothDeviceService!!.moveHubController.deactivateColorSensorNotifications()
            }
        }
        checkbox_tilt_sensor.setOnClickListener {
            if (checkbox_tilt_sensor.isChecked) {
                BluetoothDeviceService!!.moveHubController.activateTiltSensorNotifications()
            } else {
                BluetoothDeviceService!!.moveHubController.deactivateTiltSensorNotifications()
            }
        }
        checkbox_external_motor.setOnClickListener {
            if (checkbox_external_motor.isChecked) {
                BluetoothDeviceService!!.moveHubController.activateInternalMotorSensorsNotifications()
            } else {
                BluetoothDeviceService!!.moveHubController.deactivateInternalMotorSensorsNotifications()
            }
        }
        checkbox_internal_motors.setOnClickListener {
            if (checkbox_internal_motors.isChecked) {
                BluetoothDeviceService!!.moveHubController.activateExternalMotorSensorNotifications()
            } else {
                BluetoothDeviceService!!.moveHubController.deactivateExternalMotorSensorNotifications()
            }
        }
    }

    // TODO: Find a better way to access the BluetoothDeviceService
    fun setBluetoothDeviceService(bluetoothDeviceService: BluetoothDeviceService) {
        BluetoothDeviceService = bluetoothDeviceService
    }

    fun boostConnectionChanged(isConnected: Boolean) {
        checkbox_all_notifications.isEnabled = isConnected
        checkbox_button.isEnabled = isConnected
        checkbox_color_sensor.isEnabled = isConnected
        checkbox_tilt_sensor.isEnabled = isConnected
        checkbox_external_motor.isEnabled = isConnected
        checkbox_internal_motors.isEnabled = isConnected

        if (isConnected) {
            text_boost_connected.visibility = View.VISIBLE
        } else {
            text_boost_connected.visibility = View.INVISIBLE
        }
    }

    fun lpf2ConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            text_lpf2_connected.visibility = View.VISIBLE
        } else {
            text_lpf2_connected.visibility = View.INVISIBLE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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
    interface OnFragmentInteractionListener {
        //fun onChangeNotification(type: NotificationType, enabled: Boolean)
    }

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
