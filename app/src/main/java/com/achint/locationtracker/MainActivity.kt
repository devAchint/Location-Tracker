package com.achint.locationtracker

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
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
import com.achint.locationtracker.utils.Constants.ACTION_STOP_TRACKING
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.PermissionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var myMap: GoogleMap? = null
    private val viewModel: MainViewModel by viewModels()
    private var pathPoints = mutableListOf<LatLng>()
    private var objectLocation:LatLng?=null


    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (!permissions.containsValue(false)) {
                if (PermissionManager.hasBackgroundPermissions(this))
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

            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    PermissionManager.background
                )
            ) {

            } else {

            }
        }
    private lateinit var binding: ActivityMainBinding
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync {
            onMapReady(it)
            addAllLines()
            objectLocation?.let {
                addCircle(it)
            }

        }
        checkPermission()

        binding.startTracking.setOnClickListener {
            if (!isTracking) {
                updateTrackingService(ACTION_START_TRACKING)
            } else {
                updateTrackingService(ACTION_STOP_TRACKING)
            }
        }
        trackingObservers()
    }

    private fun trackingObservers() {
        TrackingService.isTracking.observe(this) {
            isTracking = it
            if (it) {
                binding.startTracking.text = "Stop Tracking"
                viewModel.getObjectLocation()
            } else {
                binding.startTracking.text = "Start Tracking"
            }
        }

        TrackingService.objectLocation.observe(this) {
            objectLocation=it
            addCircle(it)
        }

        TrackingService.pathPoints.observe(this) {
            this.pathPoints = it
            updatePolyLine()
        }

    }

    private fun addAllLines() {
        val polylineOptions = PolylineOptions()
            .color(Color.RED)
            .width(2f)
            .addAll(pathPoints)
        myMap?.addPolyline(polylineOptions)
    }

    private fun updateTrackingService(action: String) {
        Intent(this, TrackingService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun updatePolyLine() {
        if (pathPoints.isNotEmpty() && pathPoints.size > 1) {
            val preLastLatLng = pathPoints[pathPoints.size - 2]
            val lastLatLng = pathPoints.last()
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
            else
                backgroundPermissionLauncher.launch(PermissionManager.background)
        }
    }


    private fun addCircle(latLng: LatLng) {
        Toast.makeText(this, "circle", Toast.LENGTH_SHORT).show()
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
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        } ?: kotlin.run {
            Toast.makeText(this, "map became null", Toast.LENGTH_SHORT).show()
        }
    }

}
