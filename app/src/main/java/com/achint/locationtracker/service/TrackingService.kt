package com.achint.locationtracker.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.achint.locationtracker.GeoFencingReceiver
import com.achint.locationtracker.MainActivity
import com.achint.locationtracker.R
import com.achint.locationtracker.utils.Constants.ACTION_START_TRACKING
import com.achint.locationtracker.utils.Constants.ACTION_STOP_TRACKING
import com.achint.locationtracker.utils.Constants.GEOFENCE_REQUEST_ID
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.achint.locationtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.achint.locationtracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.achint.locationtracker.utils.Constants.NOTIFICATION_ID
import com.achint.locationtracker.utils.GeoFencingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
@SuppressLint("MissingPermission")
class TrackingService : LifecycleService() {
    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<MutableList<LatLng>>()
        val currentLocation = MutableLiveData<LatLng>()
        val objectLocation = MutableLiveData<LatLng>()
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geoFencingClient: GeofencingClient
    private lateinit var geoFenceUtils: GeoFencingUtils
    private fun initialize() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    var isActive = false

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        initialize()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geoFencingClient = LocationServices.getGeofencingClient(this)
       geoFenceUtils = GeoFencingUtils(this)
        isTracking.observe(this) {
            updateLocationTracking(it)
        }
//        pathPoints.observe(this) {
//            if (it.isNotEmpty() && it.size > 1) {
//                addGeoFenceClient(LatLng(it.first().latitude, it.first().longitude))
//            }
//        }

    }


    private fun addGeoFenceClient(latLng: LatLng) {
        geoFencingClient.addGeofences(
            geoFenceUtils.getGeofencingRequest(latLng),
            geoFenceUtils.geofencePendingIntent
        ).run {
            addOnSuccessListener {
                Timber.d("Geofence added successfully.")
            }
            addOnFailureListener { exception ->
                Timber.e("Geofence failed to add: ${exception.message}")
            }
        }
    }
    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeoFencingReceiver::class.java)
        Timber.d("Geo pend")
        PendingIntent.getBroadcast(
            this,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT  or PendingIntent.FLAG_MUTABLE
        )
    }

    fun getGeofencingRequest(latLng: LatLng): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(getGeoFence(latLng))
        }.build()
    }

    private fun getGeoFence(latLng: LatLng): Geofence {
        return Geofence.Builder().setRequestId(GEOFENCE_REQUEST_ID)
            .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCING_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Never expire
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT) // Listen for both enter and exit transitions
            .build()
    }
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            val request = LocationRequest.Builder(1000).apply {
                setPriority(PRIORITY_HIGH_ACCURACY)
            }.build()
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }

    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                for (location in result.locations) {
                    addPathPoint(location)
                    //Timber.d("Geo points ${location.latitude}:${location.longitude}")
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            pathPoints.value?.apply {
                add(latLng)
                pathPoints.postValue(this)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_TRACKING -> {
                    if (!isActive) {
                        isActive = true
                        Timber.d("start")
                        startForegroundService()
                    }
                }

                ACTION_STOP_TRACKING -> {
                    isActive = false
                    isTracking.postValue(false)
                    //stopForegroundService()
                }
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        isTracking.postValue(true)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                objectLocation.postValue(LatLng(location.latitude, location.longitude))
                addGeoFenceClient(LatLng(location.latitude, location.longitude))
//                myMap?.let { myMap ->
//                    val latLng = LatLng(it.latitude, it.longitude)
//                    myMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
//                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))
//                }
            }

        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking your location")
            .setContentText("Location tracking is active. Tap here for more information.")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(getPendingIntent())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val notificationChannel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}