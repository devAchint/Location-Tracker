package com.achint.locationtracker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.achint.locationtracker.utils.Constants.ALERT_NOTIFICATION_ID
import com.achint.locationtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeoFencingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "intent", Toast.LENGTH_SHORT).show()
        Timber.d("Geo intent received $intent")
        intent?.let {

            val geofencingEvent = GeofencingEvent.fromIntent(it)
            // geofencingEvent?.let { event ->
            if (geofencingEvent?.hasError() == true) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Timber.d("Geofence receiver $errorMessage")
                return
            }
            Timber.d("Geo event is $geofencingEvent")
            val geofenceTransition = geofencingEvent?.geofenceTransition
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                showAlert(context!!,"You have entered the radius")
                Timber.d("Geo enter")
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                showAlert(context!!,  "You have left the radius")
                Timber.d("Geo exit")
            } else {
                Timber.d("Geo $geofenceTransition")
            }
            //  }?:run {
            // }
        }


    }

    private fun playAudio(context: Context?) {
        val media = MediaPlayer.create(context, R.raw.alert)
        media.start()
    }

    private fun showAlert(context: Context, description:String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        playAudio(context)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}