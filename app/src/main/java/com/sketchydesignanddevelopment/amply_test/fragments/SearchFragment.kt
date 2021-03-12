package com.sketchydesignanddevelopment.amply_test.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.sketchydesignanddevelopment.amply_test.R
import com.squareup.picasso.Picasso
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class SearchFragment: Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: EditText
    private lateinit var iconView: ImageView
    private lateinit var locationView: TextView
    private lateinit var currentTempView: TextView
    private lateinit var highTempView: TextView
    private lateinit var lowTempView: TextView
    private lateinit var conditionsView: TextView
    private lateinit var searchButton: Button

    private lateinit var iconString: String
    private lateinit var locationString: String
    private lateinit var currentTempString: String
    private lateinit var highTempString: String
    private lateinit var lowTempString: String
    private lateinit var conditionsString: String

    //new instance
    companion object {
        fun newInstance():SearchFragment {
            return SearchFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = activity!!.findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        searchEditText = activity!!.findViewById(R.id.zipCodeEditText)
        iconView = activity!!.findViewById(R.id.currentWeatherIconImageView)
        locationView = activity!!.findViewById(R.id.searchLocationDetail)
        currentTempView = activity!!.findViewById(R.id.searchCurrentTempDetail)
        highTempView = activity!!.findViewById(R.id.searchHighTempDetail)
        lowTempView = activity!!.findViewById(R.id.searchLowTempDetail)
        conditionsView = activity!!.findViewById(R.id.searchConditionsDetail)
        searchButton = activity!!.findViewById(R.id.searchButton)

        searchButton.setOnClickListener {

            val zip = searchEditText.text.toString()
            if(zip.trim().isNotEmpty()){
                jSONRequest(zip)
            }else{
                Toast.makeText(context, "Please enter a valid zip code!",Toast.LENGTH_SHORT).show()
            }

        }
    }

    //getting weather data from user entered zip code
    private fun jSONRequest(zip: String){

        progressBar.visibility = View.VISIBLE

        Executors.newSingleThreadExecutor().execute{

            val url = "https://j9l4zglte4.execute-api.us-east-1.amazonaws.com/api/ctl/weather"

            val json: MediaType? = MediaType.parse("application/json; charset=utf-8")

            val jsonObjectString = "{\"zipcode\":$zip}"

            val body: RequestBody = RequestBody.create(json, jsonObjectString)
            val request = Request.Builder().post(body).url(url).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val weatherData = response.body()?.string()
                    val jsonObject = JSONObject(weatherData)
                    val jsonDetail: JSONObject = jsonObject.get("today") as JSONObject
                    iconString = jsonDetail.get("iconLink") as String
                    locationString = jsonDetail.get("city") as String + ", " + jsonDetail.get("state") as String
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
                        progressBar.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("Failed", "FAILED")
                    e.printStackTrace()
                }
            })
        }
    }
}