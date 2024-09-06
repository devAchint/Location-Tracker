package com.achint.locationtracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object PermissionManager {
    val locationPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }

    }.toTypedArray()

    val background = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun hasLocationPermissions(context: Context): Boolean {
        for (permission in locationPermissions) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun hasBackgroundPermissions(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                background
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return true
    }
}



