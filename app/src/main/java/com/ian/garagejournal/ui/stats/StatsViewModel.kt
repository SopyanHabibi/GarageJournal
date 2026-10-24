package com.ian.garagejournal.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.EfficiencyPoint
import com.ian.garagejournal.domain.FuelCalculator
import com.ian.garagejournal.domain.MonthlyCost
import com.ian.garagejournal.domain.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsState(
    val efficiencyPoints: List<EfficiencyPoint> = emptyList(),
    val overallEfficiency: Double? = null,
    val latestEfficiency: Double? = null,
    val bestEfficiency: Double? = null,
    val monthly: List<MonthlyCost> = emptyList(), // sesuai pilihan 6 atau 12 bulan
    val yearTotal: Long = 0L,
    val averagePerMonth: Long = 0L
)

class StatsViewModel(repo: GarageRepository, vehicleId: Long) : ViewModel() {

    private val _months = MutableStateFlow(6)
    val months: StateFlow<Int> = _months.asStateFlow()

    val state: StateFlow<StatsState> = combine(
        repo.observeFuelLogs(vehicleId),
        repo.observeServiceLogs(vehicleId),
        _months
    ) { fuel, service, months ->
        val now = System.currentTimeMillis()
        val points = StatsCalculator.efficiencyPoints(fuel)
        val lastYear = StatsCalculator.monthlyCosts(service, fuel, 12, now)
        StatsState(
            efficiencyPoints = points,
            overallEfficiency = FuelCalculator.overall(fuel),
            latestEfficiency = points.lastOrNull()?.kmPerLiter,
            bestEfficiency = points.maxOfOrNull { it.kmPerLiter },
            monthly = lastYear.takeLast(months),
            yearTotal = lastYear.sumOf { it.total },
            averagePerMonth = StatsCalculator.averageMonthlyCost(lastYear)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun selectMonths(value: Int) {
        _months.value = value
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { StatsViewModel(repo, vehicleId) } }
    }
}