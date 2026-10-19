package com.ian.garagejournal.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderFormState(
    val name: String = "",
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val lastDoneOdometer: String = "",
    val lastDoneDateMillis: Long = System.currentTimeMillis(),
    val isEditing: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class ReminderFormViewModel(
    private val repo: GarageRepository,
    private val vehicleId: Long,
    private val reminderId: Long // -1 = mode tambah
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderFormState(isEditing = reminderId != -1L))
    val state: StateFlow<ReminderFormState> = _state.asStateFlow()

    private var original: Reminder? = null
    private var vehicleOdometer: Int = 0

    init {
        viewModelScope.launch {
            vehicleOdometer = repo.observeVehicle(vehicleId).first()?.currentOdometer ?: 0
            if (reminderId != -1L) {
                repo.getReminder(reminderId)?.let { r ->
                    original = r
                    _state.update {
                        it.copy(
                            name = r.name,
                            intervalKm = r.intervalKm?.toString() ?: "",
                            intervalMonths = r.intervalMonths?.toString() ?: "",
                            lastDoneOdometer = r.lastDoneOdometer.toString(),
                            lastDoneDateMillis = r.lastDoneDate
                        )
                    }
                }
            } else {
                _state.update { it.copy(lastDoneOdometer = vehicleOdometer.toString()) }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onIntervalKmChange(v: String) = _state.update { it.copy(intervalKm = v.filter(Char::isDigit), error = null) }
    fun onIntervalMonthsChange(v: String) = _state.update { it.copy(intervalMonths = v.filter(Char::isDigit), error = null) }
    fun onLastOdometerChange(v: String) = _state.update { it.copy(lastDoneOdometer = v.filter(Char::isDigit), error = null) }
    fun onDateChange(millis: Long) = _state.update { it.copy(lastDoneDateMillis = millis, error = null) }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    fun save() {
        val s = _state.value
        val name = s.name.trim()
        if (name.isEmpty()) return fail("Nama pengingat wajib diisi")

        val km: Int? = if (s.intervalKm.isBlank()) null
        else s.intervalKm.toIntOrNull()?.takeIf { it > 0 } ?: return fail("Interval km harus lebih dari 0")

        val months: Int? = if (s.intervalMonths.isBlank()) null
        else s.intervalMonths.toIntOrNull()?.takeIf { it in 1..120 } ?: return fail("Interval bulan harus antara 1 dan 120")

        if (km == null && months == null) return fail("Isi interval km, interval bulan, atau keduanya")

        val lastOdo = s.lastDoneOdometer.toIntOrNull() ?: return fail("Odometer terakhir wajib diisi")
        if (lastOdo > vehicleOdometer) {
            return fail("Odometer terakhir tidak boleh melebihi odometer motor (${"%,d".format(vehicleOdometer)} km)")
        }
        if (s.lastDoneDateMillis > System.currentTimeMillis()) {
            return fail("Tanggal terakhir tidak boleh di masa depan")
        }

        viewModelScope.launch {
            val reminder = Reminder(
                id = original?.id ?: 0,
                vehicleId = vehicleId,
                name = name,
                intervalKm = km,
                intervalMonths = months,
                lastDoneOdometer = lastOdo,
                lastDoneDate = s.lastDoneDateMillis
            )
            if (original == null) repo.addReminder(reminder) else repo.updateReminder(reminder)
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long, reminderId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { ReminderFormViewModel(repo, vehicleId, reminderId) } }
    }
}