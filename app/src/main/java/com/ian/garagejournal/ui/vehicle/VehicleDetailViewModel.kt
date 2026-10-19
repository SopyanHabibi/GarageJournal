package com.ian.garagejournal.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.ReminderCalculator
import com.ian.garagejournal.domain.ReminderWithStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehicleDetailViewModel(
    private val repo: GarageRepository,
    vehicleId: Long
) : ViewModel() {

    val vehicle: StateFlow<Vehicle?> = repo.observeVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Pengingat diurutkan dari yang paling mendesak
    val reminders: StateFlow<List<ReminderWithStatus>> = combine(
        repo.observeVehicle(vehicleId),
        repo.observeReminders(vehicleId)
    ) { v, list ->
        val odometer = v?.currentOdometer ?: 0
        val now = System.currentTimeMillis()
        list.map { ReminderWithStatus(it, ReminderCalculator.status(it, odometer, now)) }
            .sortedWith(
                compareByDescending<ReminderWithStatus> { it.status.level }
                    .thenBy { it.reminder.name }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markDone(reminder: Reminder) {
        val odometer = vehicle.value?.currentOdometer ?: return
        viewModelScope.launch {
            repo.markReminderDone(reminder.id, reminder.vehicleId, odometer, System.currentTimeMillis())
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { repo.deleteReminder(reminder) }
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { VehicleDetailViewModel(repo, vehicleId) } }
    }
}