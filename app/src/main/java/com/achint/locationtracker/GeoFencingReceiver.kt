package com.achint.locationtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeoFencingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val geofencingEvent = GeofencingEvent.fromIntent(it)
           // geofencingEvent?.let { event ->
                if (geofencingEvent?.hasError()==true) {
                    val errorMessage = GeofenceStatusCodes
                        .getStatusCodeString(geofencingEvent.errorCode)
                    Timber.d("Geofence receiver $errorMessage")
                    return
                }
                val geofenceTransition = geofencingEvent?.geofenceTransition
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Timber.d("Geo enter")
                } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Timber.d("Geo exit")
                }else{
                    Timber.d("Geo $geofenceTransition")
                }
          //  }?:run {
           // }
        }


    }
}