package com.achint.locationtracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.achint.locationtracker.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var myMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        checkPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap
        myMap?.isMyLocationEnabled = true
        myMap?.uiSettings?.isMyLocationButtonEnabled =true
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

}