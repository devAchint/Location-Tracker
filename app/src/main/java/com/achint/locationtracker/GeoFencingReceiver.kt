package com.achint.locationtracker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.achint.locationtracker.utils.Constants.ALERT_NOTIFICATION_ID
import com.achint.locationtracker.utils.Constants.ALERT_NOTIFICATION_PENDING_INTENT_REQUEST_CODE
import com.achint.locationtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeoFencingReceiver : BroadcastReceiver() {
    companion object {
        var isObjectUnderRadius = MutableLiveData<Boolean>()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val geofencingEvent = GeofencingEvent.fromIntent(it)
            if (geofencingEvent?.hasError() == true) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Timber.d("Geofence receiver $errorMessage")
                return
            }
            val geofenceTransition = geofencingEvent?.geofenceTransition
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    showAlert(context!!, "You have entered the radius")
                    isObjectUnderRadius.postValue(true)
                    Timber.d("Geo enter")
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    showAlert(context!!, "You have left the radius")
                    isObjectUnderRadius.postValue(false)
                    Timber.d("Geo exit")
                }

                else -> {
                    Timber.d("Geo $geofenceTransition")
                }
            }
        }

    }

    private fun playAudio(context: Context?) {
        val media = MediaPlayer.create(context, R.raw.alert)
        media.start()
    }

    private fun showAlert(context: Context, description: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        playAudio(context)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle("Tracking your location")
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setContentIntent(getPendingIntent(context))
            .build()
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(context: Context?) =
        PendingIntent.getActivity(
            context,
            ALERT_NOTIFICATION_PENDING_INTENT_REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}