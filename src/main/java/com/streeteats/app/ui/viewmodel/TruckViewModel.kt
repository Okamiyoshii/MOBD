package com.streeteats.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streeteats.app.model.FoodTruck
import com.streeteats.app.model.MenuItem
import com.streeteats.app.network.RetrofitClient
import com.streeteats.app.network.StreetEatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class TruckViewModel : ViewModel() {
    private val repository = StreetEatsRepository(RetrofitClient.api)

    private val _truckState = MutableStateFlow<FoodTruck?>(null)
    val truckState: StateFlow<FoodTruck?> = _truckState

    fun fetchTruckDetails(truckId: String) {
        viewModelScope.launch {
            repository.getTruckDetails(truckId).collect { truck ->
                _truckState.value = truck
            }
        }
    }

    fun addMenuItem(item: MenuItem, imageFile: File? = null) {
        viewModelScope.launch {
            val success = repository.addMenuItem(item, imageFile)
            if (success) {
                fetchTruckDetails(item.truckId)
            }
        }
    }
}
