package com.achint.locationtracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.achint.locationtracker.databinding.ActivityMainBinding
import com.achint.locationtracker.service.TrackingService
import com.achint.locationtracker.utils.Constants.ACTION_START_TRACKING
import com.achint.locationtracker.utils.Constants.ACTION_STOP_TRACKING
import com.achint.locationtracker.utils.Constants.GEOFENCE_FILL_COLOR
import com.achint.locationtracker.utils.Constants.GEOFENCE_STROKE_COLOR
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.Constants.POLYLINE_COLOR
import com.achint.locationtracker.utils.Constants.POLYLINE_WIDTH
import com.achint.locationtracker.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private var myMap: GoogleMap? = null
    private var pathPoints = mutableListOf<LatLng>()
    private var objectLocation: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (!permissions.containsValue(false)) {
                loadMap()
            } else {
                val anyRationaleNeeded = PermissionManager.locationPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }
                if (anyRationaleNeeded) {
                    showRationale(false, false)
                } else {
                    showRationale(true, false)
                }
            }
        }


    private val backgroundPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {

            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    PermissionManager.backgroundLocationPermissions
                )
            ) {
                showRationale(false, true)
            } else {
                showRationale(true, true)
            }
        }
    private lateinit var binding: ActivityMainBinding
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        checkLocationPermissions()
        setOnClickListeners()
        trackingObservers()
    }

    private fun loadMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync {
            myMap = it
            myMap?.isMyLocationEnabled = true
            myMap?.uiSettings?.isMyLocationButtonEnabled = true
            addAllLines()
            objectLocation?.let {
                addCircle(it)
            }

        }
    }

    private fun setOnClickListeners() {
        binding.startTracking.setOnClickListener {
            if (PermissionManager.hasLocationPermissions(this)) {
                if (PermissionManager.hasBackgroundPermissions(this)) {
                    if (!isTracking) {
                        updateTrackingService(ACTION_START_TRACKING)
                    } else {
                        updateTrackingService(ACTION_STOP_TRACKING)
                    }
                } else {
                    backgroundPermissionLauncher.launch(PermissionManager.backgroundLocationPermissions)
                }
            } else {
                locationPermissionLauncher.launch(PermissionManager.locationPermissions)
            }
        }
    }

    private fun trackingObservers() {
        TrackingService.isTracking.observe(this) {
            isTracking = it
            if (it) {
                binding.startTracking.text = "Stop Tracking"
            } else {
                binding.startTracking.text = "Start Tracking"
            }
        }

        TrackingService.objectLocation.observe(this) {
            objectLocation = it
            it?.let {
                addCircle(it)
            } ?: kotlin.run {
                myMap?.clear()
            }
        }

        TrackingService.pathPoints.observe(this) {
            this.pathPoints = it
            updatePolyLine()
            moveToCamera()
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                myMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                myMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }

    }

    private fun addAllLines() {
        val polylineOptions = PolylineOptions()
            .color(POLYLINE_COLOR)
            .width(POLYLINE_WIDTH)
            .addAll(pathPoints)
        myMap?.addPolyline(polylineOptions)
    }

    private fun updateTrackingService(action: String) {
        Intent(this, TrackingService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun moveToCamera() {
        if (pathPoints.isNotEmpty() && pathPoints.size > 1) {
            myMap?.moveCamera(CameraUpdateFactory.newLatLng(pathPoints.last()))
            myMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 16f))
        }
    }

    private fun updatePolyLine() {
        if (pathPoints.isNotEmpty() && pathPoints.size > 1) {
            val preLastLatLng = pathPoints[pathPoints.size - 2]
            val lastLatLng = pathPoints.last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            myMap?.addPolyline(polylineOptions)
        }
    }


    private fun checkLocationPermissions() {
        if (PermissionManager.hasLocationPermissions(this).not()) {
            locationPermissionLauncher.launch(PermissionManager.locationPermissions)
        } else {
            loadMap()
        }
    }


    private fun addCircle(latLng: LatLng) {
        val marker = MarkerOptions().position(latLng).title("Object location").icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        )
        val circle = CircleOptions()
            .center(latLng)
            .radius(GEOFENCING_RADIUS.toDouble())
            .fillColor(GEOFENCE_FILL_COLOR)
            .strokeColor(GEOFENCE_STROKE_COLOR)
        myMap?.let {
            it.addMarker(marker)
            it.addCircle(circle)
            it.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    private fun showRationale(isSetting: Boolean, isBackgroundPermission: Boolean) {
        val positiveButton = if (isSetting) "Settings" else "Ok"
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs location permission to fetch current location.Please give access to all time location permission ")
            .setPositiveButton(positiveButton) { _, _ ->
                if (isSetting.not()) {
                    if (isBackgroundPermission) {
                        backgroundPermissionLauncher.launch(PermissionManager.backgroundLocationPermissions)
                    } else {
                        locationPermissionLauncher.launch(PermissionManager.locationPermissions)
                    }
                } else {
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    override fun onRestart() {
        super.onRestart()
        checkLocationPermissions()
    }


}
