package com.ian.garagejournal.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class VehicleDetailViewModel(repo: GarageRepository, vehicleId: Long) : ViewModel() {

    val vehicle: StateFlow<Vehicle?> = repo.observeVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { VehicleDetailViewModel(repo, vehicleId) } }
    }
}