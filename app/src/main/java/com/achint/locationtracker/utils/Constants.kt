package com.achint.locationtracker.utils

import androidx.datastore.preferences.core.doublePreferencesKey

object Constants {
    const val USER_PREFERENCES = "user_preferences"
    const val GEOFENCING_RADIUS = 2000F
    val OBJECT_LAT_KEY = doublePreferencesKey("OBJECT_LAT_KEY")
    val OBJECT_LONG_KEY = doublePreferencesKey("OBJECT_LONG_KEY")


    const val NOTIFICATION_CHANNEL_ID="TRACKING_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME="TRACKING"
    const val NOTIFICATION_ID=1
}