package com.nateswartz.boostcontroller.fragments

import android.content.Context
import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nateswartz.boostcontroller.R
import kotlinx.android.synthetic.main.fragment_sphero.*

class SpheroFragment: Fragment() {
    private var listener: SpheroFragmentListener? = null
    private var isSpheroServiceBound = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sphero, container, false)
    }

    override fun onStart() {
        super.onStart()

        switch_sphero_color_button.isEnabled = false
        switch_sphero_color_tilt.isEnabled = false

        button_sphero_connect.setOnClickListener {
            if (isSpheroServiceBound) {
                listener!!.onDisconnect()
            } else {
                listener!!.onConnect()
            }
            button_sphero_connect.isEnabled = false
        }

        switch_sphero_color_button.setOnClickListener {
            listener!!.onButtonLEDListenerChange(switch_sphero_color_button.isChecked)
        }

        switch_sphero_color_tilt.setOnClickListener {
            listener!!.onTiltLEDListenerChange(switch_sphero_color_tilt.isChecked)
        }
    }

    fun spheroOnline() {
        switch_sphero_color_button.isEnabled = true
        switch_sphero_color_tilt.isEnabled = true
        button_sphero_connect.text = "Disconnect Sphero"
        button_sphero_connect.isEnabled = true
    }

    fun spheroOffline() {
        button_sphero_connect.text = "Connect Sphero"
        button_sphero_connect.isEnabled = true
    }

    fun spheroServiceBound() {
        isSpheroServiceBound = true
    }

    fun spheroServiceUnbound() {
        isSpheroServiceBound = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SpheroFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement SpheroFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface SpheroFragmentListener {
        fun onConnect()
        fun onDisconnect()
        fun onButtonLEDListenerChange(enabled: Boolean)
        fun onTiltLEDListenerChange(enabled: Boolean)
    }
}
