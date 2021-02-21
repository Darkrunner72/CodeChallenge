package com.example.codechallenge

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // elements
    private lateinit var mMap: GoogleMap
    private lateinit var marker: Marker
    private lateinit var mapContainer: RelativeLayout
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var search: EditText
    private lateinit var searchBtn: Button
    private lateinit var resScreen: RelativeLayout
    private lateinit var btnBack: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnDayContainer: LinearLayout
    private lateinit var btnDay1: LinearLayout
    private lateinit var btnDay2: LinearLayout
    private lateinit var btnDay3: LinearLayout
    private lateinit var btnDay4: LinearLayout
    private lateinit var btnDay5: LinearLayout
    private lateinit var slider: Slider
    private lateinit var sliderContainer: LinearLayout
    private lateinit var sliderTitle: TextView
    private lateinit var sliderText: TextView
    // variables
    private val PERM_REQUEST_CODE = 101
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    private lateinit var GPS_LOC: LatLng
    private val OWM_API: String = "14af095c2524017e1cc1e5228e16731e"
    private var OWM_CITY: String = "Singapore,SG"
    private var OWM_DAY: Int = 0
    private var OWM_TIME: Int = 0
    private lateinit var OWM_CITIES: List<City>
    val searchResults: ArrayList<SearchResult> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)


        bindElements()
        bindEvents()

        initApp()
    }

    private fun bindElements() {
        mapContainer = findViewById<RelativeLayout>(R.id.mapContainer)

        linearLayoutManager = LinearLayoutManager(this)
        rvSearchResults = findViewById<RecyclerView>(R.id.rvSearchResults)
            linearLayoutManager = LinearLayoutManager(this)
            // Creates a vertical Layout Manager
            rvSearchResults.layoutManager = LinearLayoutManager(this)
            // Access the RecyclerView Adapter and load the data into it

        search = findViewById<EditText>(R.id.etSearch)
        searchBtn = findViewById<Button>(R.id.btnSearch)
        resScreen = findViewById<RelativeLayout>(R.id.resScreen)
        btnBack = findViewById<Button>(R.id.btnBack)
        btnPrev = findViewById<Button>(R.id.btnPrev)
        btnNext = findViewById<Button>(R.id.btnNext)
        btnDayContainer = findViewById<LinearLayout>(R.id.btnDayContainer)
        btnDay1 = findViewById<LinearLayout>(R.id.btnDay1)
        btnDay2 = findViewById<LinearLayout>(R.id.btnDay2)
        btnDay3 = findViewById<LinearLayout>(R.id.btnDay3)
        btnDay4 = findViewById<LinearLayout>(R.id.btnDay4)
        btnDay5 = findViewById<LinearLayout>(R.id.btnDay5)
        slider = findViewById<Slider>(R.id.slider)
        sliderContainer = findViewById<LinearLayout>(R.id.sliderContainer)
        sliderTitle = findViewById<TextView>(R.id.sliderTitle)
        sliderText = findViewById<TextView>(R.id.sliderText)
    }

    private fun bindEvents() {
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJSON(search.text.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        searchBtn.setOnClickListener{
            searchOWM(search.text.toString())
        }

        btnBack.setOnClickListener{
            loadHomeView()
        }

        btnPrev.setOnClickListener{
            if(OWM_DAY>0){
                OWM_DAY--
            }
            loadResView()
        }
        btnNext.setOnClickListener{
            if(OWM_DAY<4){
                OWM_DAY++
            }
            loadResView()
        }
        btnDay1.setOnClickListener{
            OWM_DAY = 0
            loadResView()
        }
        btnDay2.setOnClickListener{
            OWM_DAY = 1
            loadResView()
        }
        btnDay3.setOnClickListener{
            OWM_DAY = 2
            loadResView()
        }
        btnDay4.setOnClickListener{
            OWM_DAY = 3
            loadResView()
        }
        btnDay5.setOnClickListener{
            OWM_DAY = 4
            loadResView()
        }

        slider.addOnChangeListener { slider, value, fromUser ->
            OWM_TIME=value.roundToInt()
            printTime()
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                loadResView()
            }
        })
    }

    private fun initApp(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Init location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        initCITIES()
    }

    private fun initCITIES(){
        var jsonString: String
        try {
            Log.d("Debug:","File:cities.json, initializing... ")
            jsonString = assets.open("cities.json").bufferedReader().use { it.readText() }
            val stringReader: StringReader = StringReader(jsonString)
            val gsonBuilder: GsonBuilder = GsonBuilder().serializeNulls()
            val gson: Gson = gsonBuilder.create()
            val cities: List<City> = gson.fromJson(stringReader , Array<City>::class.java).toList()
            OWM_CITIES = cities
            Log.d("Debug:","File:cities.json, successfully initialized. ")
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener { marker ->
            if (marker.isInfoWindowShown) {
                marker.hideInfoWindow()
            } else {
                marker.showInfoWindow()
            }
            update_OWM_CITY()
            loadResView()
            true
        }

        getLastLocation()
    }

    private fun loadResView() {
        resScreen.visibility = View.VISIBLE
        mapContainer.visibility = View.GONE
        toggleDays()
        printTime()
        weatherTask().execute()
    }
    private fun toggleDays(){
        if(OWM_DAY==0){
            btnPrev.visibility = View.GONE
        }else{
            btnPrev.visibility = View.VISIBLE
        }
        if(OWM_DAY==4) {
            btnNext.visibility = View.GONE
        }else{
            btnNext.visibility = View.VISIBLE
        }
        btnDay1.setBackgroundColor(0)
        btnDay2.setBackgroundColor(0)
        btnDay3.setBackgroundColor(0)
        btnDay4.setBackgroundColor(0)
        btnDay5.setBackgroundColor(0)
        when(OWM_DAY){
            0 -> {
                btnDay1.setBackgroundColor(0x3CF1EBF1)
            }
            1 -> {
                btnDay2.setBackgroundColor(0x3CF1EBF1)
            }
            2 -> {
                btnDay3.setBackgroundColor(0x3CF1EBF1)
            }
            3 -> {
                btnDay4.setBackgroundColor(0x3CF1EBF1)
            }
            4 -> {
                btnDay5.setBackgroundColor(0x3CF1EBF1)
            }
            else  -> {
                OWM_DAY=0
                toggleDays()
            }
        }
    }
    fun printTime(){
        var date = Date()
        date.hours = OWM_TIME * 3
        sliderText.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(date)
    }

    fun loadHomeView() {
        resScreen.visibility = View.GONE
        findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
        mapContainer.visibility = View.VISIBLE
        getLastLocation()
    }

    fun CheckPermission():Boolean{
        if(
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            return true
        }
        return false
    }
    fun RequestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERM_REQUEST_CODE
        )
    }
    fun isLocationEnabled():Boolean{
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERM_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d("Debug:", "Permission has been denied by user")
                } else {
                    Log.d("Debug:", "Permission has been granted by user")
                    getLastLocation()
                }
            }
        }
    }

    fun getLastLocation(){
        if(CheckPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task->
                    var location:Location? = task.result
                    if(location == null){
                        NewLocationData()
                    }else{
                        Log.d(
                            "Debug:",
                            "Your Location:" + location.latitude + ", " + location.longitude
                        )
                        handleCurrLoc(location)
                    }
                }
            }else{
                Toast.makeText(this, "Please Turn on Your device Location", Toast.LENGTH_SHORT).show()
            }
        }else{
            RequestPermission()
        }
    }

    fun NewLocationData(){
        var locationRequest =  LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            var lastLocation: Location = locationResult.lastLocation
            Log.d("Debug:", "your last last location: " +lastLocation.latitude.toString()+", "+lastLocation.longitude.toString())
        }
    }

    private fun getCityName(lat: Double, long: Double):String{
        var cityName:String = ""
        var countryName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Adress = geoCoder.getFromLocation(lat, long, 3)

        cityName = Adress.get(0).locality
        countryName = Adress.get(0).countryName
        Log.d("Debug:", "Your City: " + cityName + " ; your Country " + countryName)
        return cityName
    }


    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        } override fun doInBackground(vararg params: String?): String? {
            var response:String?
            try{
                response = URL("https://api.openweathermap.org/data/2.5/forecast?q=$OWM_CITY&units=metric&appid=$OWM_API").readText(
                    Charsets.UTF_8
                )
            }catch (e: Exception){
                response = null
            }
            return response
        } override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                /* Extracting JSON returns from the API */
                val jsonObj = JSONObject(result.toString())

                val city = jsonObj.getJSONObject("city")
                    val address = city.getString("name")+", "+city.getString("country")
                    val timeZone:Long = city.getLong("timezone")
                    val sunrise:Long = city.getLong("sunrise")
                    val sunset:Long = city.getLong("sunset")

                val list: JSONArray = jsonObj.getJSONArray("list")
                var success: Boolean = false
                val sample = list.getJSONObject(0)
                val datasetDate = Date(sample.getLong("dt") * 1000)
                val offset = datasetDate.hours.toInt()
                Log.d("Debug:", "Dataset Date: " + offset.toString())
                for(i in 0 until list.length()){
                    if(i==(((OWM_DAY*8)+OWM_TIME)-(offset/3))){
                        Log.d("Debug:", "Index: " + i)
                        printWeather(list.getJSONObject(i))
                        success = true;
                    }
                }
                if(!success){
                    printWeather(list.getJSONObject(0))
                    slider.value = ((offset.toFloat()/3)-1)
                }

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat(
                    "hh:mm aa",
                    Locale.ENGLISH
                ).format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat(
                    "hh:mm aa",
                    Locale.ENGLISH
                ).format(Date(sunset * 1000))

                /* Views populated, Hiding the loader, Showing the main design */
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
            }
        }
    }
    private fun printWeather(rec: JSONObject){
        var currDate = Date()
        var dt = rec.getLong("dt")
        var date = Date(dt * 1000)

        val day = SimpleDateFormat("dd EEEE, hh:mm aa", Locale.ENGLISH).format(date)
        val dateText = "Updated at: "+SimpleDateFormat(
            "dd/MM/yyyy hh:mm a", Locale.ENGLISH
        ).format(currDate)
        findViewById<TextView>(R.id.day).text = day
        findViewById<TextView>(R.id.updated_at).text =  dateText

        val main = rec.getJSONObject("main")
            val tempStr = main.getString("temp")
            val temp = tempStr.take(2)+"°C"
            val tempMin = "Min Temp: " + main.getString("temp_min")+"°C"
            val tempMax = "Max Temp: " + main.getString("temp_max")+"°C"
            val pressure = main.getString("pressure")
            val humidity = main.getString("humidity")
            findViewById<TextView>(R.id.temp).text = temp
            findViewById<TextView>(R.id.temp_min).text = tempMin
            findViewById<TextView>(R.id.temp_max).text = tempMax
            findViewById<TextView>(R.id.pressure).text = pressure
            findViewById<TextView>(R.id.humidity).text = humidity

        val wind = rec.getJSONObject("wind")
            val windSpeed = wind.getString("speed")
            findViewById<TextView>(R.id.wind).text = windSpeed

        val weather = rec.getJSONArray("weather").getJSONObject(0)
            val weatherDescription = weather.getString("description")
            findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
    }


    private fun handleCurrLoc(location: Location){
        GPS_LOC = LatLng(location.latitude, location.longitude)
        update_OWM_CITY()
        placeMarker(GPS_LOC)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(GPS_LOC))
    }
    private fun placeMarker(loc: LatLng) {
        marker = mMap.addMarker(MarkerOptions().position(loc).title("You Are Here"))

    }

    private fun update_OWM_CITY(){
        OWM_CITY = getCityName(GPS_LOC.latitude,GPS_LOC.longitude)
    }

    private fun searchJSON(text: String){
        searchResults.clear()
        if(text.length>0){
            var count = 0
            for(city in OWM_CITIES){
                if(count<10){
                    val id:String =city.id.toString()
                    val name:String =city.name.toLowerCase()
                    val state:String =city.state.toLowerCase()
                    val country:String =city.country.toLowerCase()

                    if( id.contains(text)
                        || name.contains(text)
                        || state.contains(text)
                        || country.contains(text)){
                        val searchResult: SearchResult = SearchResult(city.name,city.state,city.country)
                        searchResults.add(searchResult)
                        count++
                    }
                }else{
                    break
                }
            }
            if(count>0){
                rvSearchResults.adapter = SearchResults(this,searchResults, this)
                rvSearchResults.visibility = View.VISIBLE
            }else{
                rvSearchResults.visibility = View.GONE
            }
        }else{
            rvSearchResults.visibility = View.GONE
        }
    }
    fun updateSearch(text: String){
        search.setText(text)
    }

    private fun searchOWM(text: String){
        if(text.length>0){
            OWM_CITY=text
            loadResView()
        }
    }
}

