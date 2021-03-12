@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.sketchydesignanddevelopment.amply_test.fragments

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sketchydesignanddevelopment.amply_test.R
import com.squareup.picasso.Picasso
import okhttp3.*
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess


class MainFragment: Fragment() {

    //lateinit variable for interface listener
    private lateinit var listener: OnButtonPressed

    //view widgets
    private lateinit var progressBar: ProgressBar
    private lateinit var iconView: ImageView
    private lateinit var locationView: TextView
    private lateinit var currentTempView: TextView
    private lateinit var highTempView: TextView
    private lateinit var lowTempView: TextView
    private lateinit var conditionsView: TextView
    private lateinit var searchByZipButton: Button

    private lateinit var iconString: String
    private lateinit var locationString: String
    private lateinit var currentTempString: String
    private lateinit var highTempString: String
    private lateinit var lowTempString: String
    private lateinit var conditionsString: String

    //vars for location
    private lateinit var client: FusedLocationProviderClient
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private lateinit var zipCode: String

    //new instance
    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        //checking that activity implements interface
        if (context is OnButtonPressed) {
            listener = context
        } else {
            throw ClassCastException(
                "$context must implement OnButtonPressed."
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //instantiating view widgets
        progressBar = activity!!.findViewById(R.id.progressBar)
        progressBar.visibility = GONE
        iconView = activity!!.findViewById(R.id.currentWeatherIconImageView)
        locationView = activity!!.findViewById(R.id.locationDetail)
        currentTempView = activity!!.findViewById(R.id.currentTempDetail)
        highTempView = activity!!.findViewById(R.id.highTempDetail)
        lowTempView = activity!!.findViewById(R.id.lowTempDetail)
        conditionsView = activity!!.findViewById(R.id.conditionsDetail)
        searchByZipButton = activity!!.findViewById(R.id.searchButton)

        searchByZipButton.setOnClickListener {
            listener.onButtonPressed()
        }

        //checking permissions
        val permissionAll = 1
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        client = LocationServices.getFusedLocationProviderClient(context)

        if (!context?.let { hasPermissions(it, *permissions) }!!) {
            activity?.let { requestPermissions(permissions, permissionAll) }
        } else {
            getWeatherDetails()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED){
            getWeatherDetails()
        }else{
            //explain to user that they need to approve permissions to proceed. If they don't approve shutdown app
            Toast.makeText(
                context,
                "You need to approve permissions to proceed with this app!",
                Toast.LENGTH_LONG
            ).show()

            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(Toast.LENGTH_LONG.toLong())
                        activity?.finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            thread.start()
        }
    }

    //fun to check multiple permissions
    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
    }

    //getting location and then weather details
    private fun getWeatherDetails() {

        progressBar.visibility = View.VISIBLE
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            //getting zip code from location
            client.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val lastLocation = task.result
                    latitude = (lastLocation)!!.latitude
                    longitude = (lastLocation).longitude
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses: ArrayList<Address> = geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    ) as ArrayList<Address>
                    zipCode = addresses[0].postalCode
                    jSONRequest(zipCode)
                } else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    Toast.makeText(
                        context,
                        "No location detected, please make sure location services is enabled.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }
    }

    //getting weather data from location info
    private fun jSONRequest(zip: String){

        Executors.newSingleThreadExecutor().execute{

            val url = "https://j9l4zglte4.execute-api.us-east-1.amazonaws.com/api/ctl/weather"

            val json: MediaType? = MediaType.parse("application/json; charset=utf-8")

            val jsonObjectString = "{\"zipcode\":$zip}"

            val body:RequestBody = RequestBody.create(json, jsonObjectString)
            val request = Request.Builder().post(body).url(url).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val weatherData = response.body()?.string()
                    val jsonObject = JSONObject(weatherData)
                    val jsonDetail: JSONObject = jsonObject.get("today") as JSONObject
                    iconString = jsonDetail.get("iconLink") as String
                    locationString =
                        jsonDetail.get("city") as String + ", " + jsonDetail.get("state") as String
                    currentTempString = jsonDetail.get("temperature") as String
                    highTempString = jsonDetail.get("highTemperature") as String
                    lowTempString = jsonDetail.get("lowTemperature") as String
                    conditionsString = jsonDetail.get("description") as String

                    //updating UI
                    requireActivity().runOnUiThread {
                        Picasso.get().load(iconString).into(iconView)
                        locationView.text = locationString
                        currentTempView.text = currentTempString
                        highTempView.text = highTempString
                        lowTempView.text = lowTempString
                        conditionsView.text = conditionsString
                        progressBar.visibility = GONE
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("Failed", "FAILED")
                    e.printStackTrace()
                }
            })
        }
    }

    //interface for changing fragments
    interface OnButtonPressed{
        fun onButtonPressed()
    }
}

