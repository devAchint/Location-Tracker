package com.achint.locationtracker.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.achint.locationtracker.R
import com.achint.locationtracker.broadcast.GeoFencingReceiver
import com.achint.locationtracker.databinding.ActivityMainBinding
import com.achint.locationtracker.service.TrackingService
import com.achint.locationtracker.utils.Constants.ACTION_START_TRACKING
import com.achint.locationtracker.utils.Constants.ACTION_STOP_TRACKING
import com.achint.locationtracker.utils.Constants.CURRENT_ZOOM
import com.achint.locationtracker.utils.Constants.GEOFENCE_FILL_COLOR
import com.achint.locationtracker.utils.Constants.GEOFENCE_STROKE_COLOR
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.Constants.MARKER_COLOR
import com.achint.locationtracker.utils.Constants.POLYLINE_COLOR
import com.achint.locationtracker.utils.Constants.POLYLINE_WIDTH
import com.achint.locationtracker.utils.Constants.TRACKING_ZOOM
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


@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {
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
                    showRationale(isSetting = false, isBackgroundPermission = false)
                } else {
                    showRationale(isSetting = true, isBackgroundPermission = false)
                }
            }
        }


    private val backgroundPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        PermissionManager.backgroundLocationPermissions
                    )
                ) {
                    showRationale(isSetting = false, isBackgroundPermission = true)
                } else {
                    showRationale(isSetting = true, isBackgroundPermission = true)
                }
            }
        }

    private var myMap: GoogleMap? = null
    private var pathPoints = mutableListOf<LatLng>()
    private var objectLocation: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setOnClickListeners()
        trackingObservers()
        checkLocationPermissions()
    }

    private fun loadMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            myMap = googleMap
            myMap?.let {
                it.isMyLocationEnabled = true
                it.uiSettings.isMyLocationButtonEnabled = true
                objectLocation?.let {
                    addFencing(it)
                }
                addAllLines()
                getLastLocation()
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
                binding.startTracking.text = getString(R.string.stop_tracking)
            } else {
                binding.startTracking.text = getString(R.string.start_tracking)
                binding.alertLayout.visibility = View.GONE
                myMap?.clear()
                getLastLocation()
            }
        }

        TrackingService.objectLocation.observe(this) {
            objectLocation = it
            it?.let {
                addFencing(it)
            }
        }

        TrackingService.pathPoints.observe(this) {
            this.pathPoints = it
            updatePolyLine()
            moveCameraToCurrentPosition()
        }
        GeoFencingReceiver.isObjectUnderRadius.observe(this) {
            if (it) {
                binding.alertLayout.visibility = View.VISIBLE
                binding.alertText.text = "Hey! You have entered the radius"
            } else {
                binding.alertLayout.visibility = View.VISIBLE
                binding.alertText.text = "Hey! You have left the radius"
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

    private fun moveCameraToCurrentPosition() {
        if (pathPoints.isNotEmpty() && pathPoints.size > 1) {
            myMap?.moveCamera(CameraUpdateFactory.newLatLng(pathPoints.last()))
            myMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last(),
                    TRACKING_ZOOM
                )
            )
        }
    }

    private fun updatePolyLine() {
        if (pathPoints.isNotEmpty() && pathPoints.size > 1) {
            val secondLastLatLng = pathPoints[pathPoints.size - 2]
            val lastLatLng = pathPoints.last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(secondLastLatLng)
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


    private fun addFencing(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng).title("Object location").icon(
            BitmapDescriptorFactory.defaultMarker(MARKER_COLOR)
        )
        val circle = CircleOptions()
            .center(latLng)
            .radius(GEOFENCING_RADIUS.toDouble())
            .fillColor(GEOFENCE_FILL_COLOR)
            .strokeColor(GEOFENCE_STROKE_COLOR)
        myMap?.let {
            it.clear()
            it.addMarker(markerOptions)
            it.addCircle(circle)
            it.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, TRACKING_ZOOM))
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

    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                myMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                myMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CURRENT_ZOOM))
            }
        }
    }

}
