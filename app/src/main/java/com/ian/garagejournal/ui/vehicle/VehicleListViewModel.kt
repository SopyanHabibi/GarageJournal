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
import kotlinx.coroutines.launch

class VehicleListViewModel(private val repo: GarageRepository) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repo.observeVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch { repo.deleteVehicle(vehicle) }
    }

    companion object {
        fun factory(repo: GarageRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VehicleListViewModel(repo) }
        }
    }
}