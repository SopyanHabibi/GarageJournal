package com.ian.garagejournal.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.FuelCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FuelSummary(
    val overallEfficiency: Double? = null,
    val latestEfficiency: Double? = null,
    val totalCost: Long = 0L,
    val totalLiters: Double = 0.0
)

class FuelListViewModel(
    private val repo: GarageRepository,
    vehicleId: Long
) : ViewModel() {

    val logs: StateFlow<List<FuelLog>> = repo.observeFuelLogs(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<FuelSummary> = logs
        .map { list ->
            FuelSummary(
                overallEfficiency = FuelCalculator.overall(list),
                // list sudah terurut dari odometer terbesar, jadi yang pertama = terbaru
                latestEfficiency = list.firstOrNull { it.kmPerLiter != null }?.kmPerLiter,
                totalCost = list.sumOf { it.totalCost },
                totalLiters = list.sumOf { it.liters }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelSummary())

    fun delete(log: FuelLog) {
        viewModelScope.launch { repo.deleteFuelLog(log) }
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { FuelListViewModel(repo, vehicleId) } }
    }
}