package com.nateswartz.boostcontroller

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray


class LifxController (context: Context){

    private val apiToken = ""
    private val lightID = ""
    private val baseUrl = "https://api.lifx.com/v1/lights/"
    private val requestQueue = Volley.newRequestQueue(context)

    fun toggleLifx() {
        val url = "${baseUrl}id:$lightID"
        val request = object : StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    val array = JSONArray(response)
                    val obj = array.getJSONObject(0)
                    if (obj["power"] == "on") {
                        changeLightPower("off")
                    } else if (obj["power"] == "off") {
                        changeLightPower("on")
                    }
                    Log.d("Volley", "Success: $response")
                },
                Response.ErrorListener {
                    // error
                    Log.e("Volley", "Error")
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $apiToken"
                return headers
            }
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["power"] = "on"
                return params
            }
        }
        requestQueue.add(request)
    }

    fun changeLightPower(state : String) {
        val url = "${baseUrl}id:$lightID/state"
        val request = object : StringRequest(Request.Method.PUT, url,
                Response.Listener<String> { response ->
                    Log.d("Volley", "Success: $response")
                },
                Response.ErrorListener {
                    // error
                    Log.e("Volley", "Error")
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $apiToken"
                return headers
            }
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["power"] = state
                return params
            }
        }
        requestQueue.add(request)
    }

    fun changeLightColor(color: String) {
        val url = "${baseUrl}id:$lightID/state"
        val request = object : StringRequest(Request.Method.PUT, url,
                Response.Listener<String> { response ->
                    Log.d("Volley", "Success: $response")
                },
                Response.ErrorListener {
                    // error
                    Log.e("Volley", "Error")
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $apiToken"
                return headers
            }
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["color"] = color
                return params
            }
        }
        requestQueue.add(request)
    }
}