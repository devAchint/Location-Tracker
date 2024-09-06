package com.achint.locationtracker

import android.annotation.SuppressLint
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
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
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

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var myMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val viewModel: MainViewModel by viewModels()
    private var objectLocation: Location? = null
    private var pathPoints = mutableListOf<Polyline>()

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (!permissions.containsValue(false)) {
                getLastLocation()
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
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        checkPermission()

        binding.startTracking.setOnClickListener {
            startTrackingService()
            objectLocation?.let {
                addGeofence(LatLng(it.latitude, it.longitude))
                addCircle(LatLng(it.latitude, it.longitude))
                viewModel.saveObjectLocation(it.latitude, it.longitude)
            }

        }
    }

    private fun startTrackingService() {
        Intent(this, TrackingService::class.java).apply {
            action = "Start"
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

    private fun addGeofence(latLng: LatLng) {
        val geofence = Geofence.Builder().setRequestId("current")
            .setCircularRegion(latLng.latitude, latLng.longitude, 2000f).build()
    }

    private fun checkPermission() {
        if (!PermissionManager.hasLocationPermissions(this)) {
            locationPermissionLauncher.launch(PermissionManager.locationPermissions)
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                objectLocation = location
                myMap?.let { myMap ->
                    val latLng = LatLng(it.latitude, it.longitude)
                    val marker = MarkerOptions().position(latLng).title("My location").icon(
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
//                    myMap.addMarker(marker)
//                    myMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
//                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
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
            .strokeColor(Color.RED)
        myMap?.let {
            it.addMarker(marker)
            it.addCircle(circle)
            it.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

}
