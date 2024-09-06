package com.achint.locationtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.achint.locationtracker.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val mainRepository: MainRepository):ViewModel() {


    fun saveObjectLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            mainRepository.saveObjectLocation(latitude,longitude)
        }
    }
}