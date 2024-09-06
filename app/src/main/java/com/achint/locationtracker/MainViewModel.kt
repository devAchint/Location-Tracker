package com.achint.locationtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.achint.locationtracker.repository.MainRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val mainRepository: MainRepository) : ViewModel() {

    private val _objectLocation = MutableStateFlow<LatLng?>(null)
    val objectLocation: StateFlow<LatLng?>
        get() = _objectLocation

    fun saveObjectLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            mainRepository.saveObjectLocation(latitude, longitude)
        }
    }

    fun getObjectLocation() {
        viewModelScope.launch {
            _objectLocation.value=mainRepository.getObjectLocation()
        }
    }
}