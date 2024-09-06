package com.achint.locationtracker.utils

import androidx.datastore.preferences.core.doublePreferencesKey

object Constants {
    const val USER_PREFERENCES = "user_preferences"
    const val GEOFENCE_REQUEST_ID = "GEOFENCE_ID"
    const val GEOFENCING_RADIUS = 100F
    val OBJECT_LAT_KEY = doublePreferencesKey("OBJECT_LAT_KEY")
    val OBJECT_LONG_KEY = doublePreferencesKey("OBJECT_LONG_KEY")


    const val NOTIFICATION_CHANNEL_ID = "TRACKING_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME = "TRACKING"
    const val NOTIFICATION_ID = 1

    const val ALERT_NOTIFICATION_ID = 2

    const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
    const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"

}