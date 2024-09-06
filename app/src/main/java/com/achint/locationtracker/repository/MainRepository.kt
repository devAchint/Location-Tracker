package com.achint.locationtracker.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.achint.locationtracker.utils.Constants.OBJECT_LAT_KEY
import com.achint.locationtracker.utils.Constants.OBJECT_LONG_KEY
import javax.inject.Inject

class MainRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    suspend fun saveObjectLocation(latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[OBJECT_LAT_KEY] = latitude
            preferences[OBJECT_LONG_KEY] = longitude
        }
    }
}