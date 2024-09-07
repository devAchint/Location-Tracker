package com.achint.locationtracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.achint.locationtracker.MainActivity
import com.achint.locationtracker.R
import com.achint.locationtracker.utils.Constants.ACTION_START_TRACKING
import com.achint.locationtracker.utils.Constants.ACTION_STOP_TRACKING
import com.achint.locationtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.achint.locationtracker.utils.Constants.NOTIFICATION_ID
import com.achint.locationtracker.utils.GeoFencingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
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
        val objectLocation = MutableLiveData<LatLng?>()
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geoFencingClient: GeofencingClient
    private lateinit var geoFenceUtils: GeoFencingUtils
    private var isActive = false

    override fun onCreate() {
        super.onCreate()
        resetValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geoFencingClient = LocationServices.getGeofencingClient(this)
        geoFenceUtils = GeoFencingUtils(this)
        isTracking.observe(this) {
            updateLocationTracking(it)
        }
    }


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
            addGeoFenceClient(objectLocation.value!!)
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeGeoFenceClient()
        }

    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                for (location in result.locations) {
                    addPathPoint(location)
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
                    stopTrackingService()
                }
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopTrackingService() {
        resetValues()
        stopForeground(true)
        stopSelf()
    }

    private fun startForegroundService() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                objectLocation.postValue(LatLng(location.latitude, location.longitude))
                isTracking.postValue(true)
            }
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle("Tracking your location")
            .setContentText("Location tracking is active. Tap here for more information.")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
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

    private fun removeGeoFenceClient(){
        geoFencingClient.removeGeofences(geoFenceUtils.geofencePendingIntent).run {
            addOnSuccessListener {
                Timber.d("Geofence removed successfully.")
            }
            addOnFailureListener { exception ->
                Timber.e("Geofence failed to remove: ${exception.message}")
            }
        }
    }

    private fun resetValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        objectLocation.postValue(null)
    }
}