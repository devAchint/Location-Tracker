package com.achint.locationtracker.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.achint.locationtracker.broadcast.GeoFencingReceiver
import com.achint.locationtracker.utils.Constants.GEOFENCE_REQUEST_ID
import com.achint.locationtracker.utils.Constants.GEOFENCING_RADIUS
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

class GeoFencingUtils(context: Context) {


     val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeoFencingReceiver::class.java)
        Timber.d("Geo pend")
        PendingIntent.getBroadcast(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT  or PendingIntent.FLAG_MUTABLE
        )
    }

     fun getGeofencingRequest(latLng: LatLng): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
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

}