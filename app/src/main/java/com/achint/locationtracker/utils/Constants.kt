package com.achint.locationtracker.utils

import android.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object Constants {
    const val GEOFENCE_REQUEST_ID = "GEOFENCE_ID"
    const val GEOFENCING_RADIUS = 10F

    const val NOTIFICATION_CHANNEL_ID = "TRACKING_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME = "TRACKING"
    const val NOTIFICATION_ID = 1
    const val ALERT_NOTIFICATION_ID = 2
    const val TRACKING_NOTIFICATION_PENDING_INTENT_REQUEST_CODE = 100
    const val ALERT_NOTIFICATION_PENDING_INTENT_REQUEST_CODE = 101

    const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
    const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"

    const val MARKER_COLOR = BitmapDescriptorFactory.HUE_RED
    const val POLYLINE_WIDTH = 5f
    const val POLYLINE_COLOR = Color.RED
    const val GEOFENCE_FILL_COLOR = 0x5587CEEB
    var GEOFENCE_STROKE_COLOR = Color.parseColor("#87CEEB")
    const val TRACKING_ZOOM = 20F
    const val CURRENT_ZOOM = 16F

}