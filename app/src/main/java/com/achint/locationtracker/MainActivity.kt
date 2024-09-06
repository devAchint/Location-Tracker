package com.achint.locationtracker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.achint.locationtracker.databinding.ActivityMainBinding
import com.achint.locationtracker.service.TrackingService
import com.achint.locationtracker.utils.Constants.ACTION_START_TRACKING
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var myMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geoFencingClient: GeofencingClient
    private val viewModel: MainViewModel by viewModels()
    private var objectLocation: Location? = null
    private var pathPoints = mutableListOf<Polyline>()

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (!permissions.containsValue(false)) {
                if (PermissionManager.hasBackgroundPermissions(this))
                    getLastLocation()
                else
                    backgroundPermissionLauncher.launch(PermissionManager.background)
            } else {
                val anyRationaleNeeded = PermissionManager.locationPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }
                if (anyRationaleNeeded) {
                    Toast.makeText(this, "rationale", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, " no rationale", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val backgroundPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getLastLocation()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    PermissionManager.background
                )
            ) {

            } else {

            }
        }
    private lateinit var binding: ActivityMainBinding
    var isTrackingStarted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geoFencingClient = LocationServices.getGeofencingClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        checkPermission()

        binding.startTracking.setOnClickListener {
            if (isTrackingStarted.not()) {
                startTrackingService()
                objectLocation?.let {
                    addGeofence(LatLng(it.latitude, it.longitude))
                    addCircle(LatLng(it.latitude, it.longitude))
                    viewModel.saveObjectLocation(it.latitude, it.longitude)
                }

                geoFencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent).run {
                    addOnSuccessListener {
                        Timber.d("Geofence added successfully.")
                    }
                    addOnFailureListener { exception ->
                        Timber.e("Geofence failed to add: ${exception.message}")
                    }
                }
                isTrackingStarted = true
            }
        }

    }

    private fun startTrackingService() {
        Intent(this, TrackingService::class.java).apply {
            action = ACTION_START_TRACKING
            startService(this)
        }
    }

    private fun updatePolyLine() {
        if (pathPoints.isNotEmpty() && pathPoints.last().points.size > 1) {
            val preLastLatLng = pathPoints.last().points[pathPoints.last().points.size - 2]
            val lastLatLng = pathPoints.last().points.last()
            val polylineOptions = PolylineOptions()
                .color(Color.RED)
                .width(2f)
                .add(preLastLatLng)
                .add(lastLatLng)
            myMap?.addPolyline(polylineOptions)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap
        myMap?.isMyLocationEnabled = true
        myMap?.uiSettings?.isMyLocationButtonEnabled = true
    }


    private fun checkPermission() {
        if (!PermissionManager.hasLocationPermissions(this)) {
            locationPermissionLauncher.launch(PermissionManager.locationPermissions)
        } else {
            if (PermissionManager.hasBackgroundPermissions(this))
                getLastLocation()
            else
                backgroundPermissionLauncher.launch(PermissionManager.background)
        }
    }

    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                objectLocation = location
                myMap?.let { myMap ->
                    val latLng = LatLng(it.latitude, it.longitude)
                    myMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))
                }
            }

        }

    }

    private fun addCircle(latLng: LatLng) {
        val marker = MarkerOptions().position(latLng).title("My location").icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        )
        val circle = CircleOptions()
            .center(latLng)
            .radius(GEOFENCING_RADIUS.toDouble())
            .fillColor(0x5500FF00)
            .strokeColor(Color.GREEN)
        myMap?.let {
            it.addMarker(marker)
            it.addCircle(circle)
            it.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))
        }
    }


    private fun addGeofence(latLng: LatLng): Geofence {
        return Geofence.Builder().setRequestId("current")
            .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCING_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Never expire
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT) // Listen for both enter and exit transitions
            .build()
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(
                addGeofence(
                    LatLng(
                        objectLocation?.latitude!!,
                        objectLocation?.longitude!!
                    )
                )
            )
        }.build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeoFencingReceiver::class.java).apply {
            action = "GEO_INTENT"
        }
        Timber.d("Geo pend")
        PendingIntent.getBroadcast(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
