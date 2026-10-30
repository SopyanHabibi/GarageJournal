package com.ian.garagejournal.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year

data class VehicleFormState(
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val plateNumber: String = "",
    val odometer: String = "",
    val isEditing: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class VehicleFormViewModel(
    private val repo: GarageRepository,
    private val vehicleId: Long // -1 = mode tambah
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleFormState(isEditing = vehicleId != -1L))
    val state: StateFlow<VehicleFormState> = _state.asStateFlow()

    private var original: Vehicle? = null

    init {
        if (vehicleId != -1L) {
            viewModelScope.launch {
                repo.observeVehicle(vehicleId).first()?.let { v ->
                    original = v
                    _state.update {
                        it.copy(
                            name = v.name,
                            brand = v.brand,
                            model = v.model,
                            year = v.year.toString(),
                            plateNumber = v.plateNumber,
                            odometer = v.currentOdometer.toString()
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onBrandChange(value: String) = _state.update { it.copy(brand = value, error = null) }
    fun onModelChange(value: String) = _state.update { it.copy(model = value, error = null) }
    fun onYearChange(value: String) = _state.update { it.copy(year = value.filter(Char::isDigit), error = null) }
    fun onPlateChange(value: String) = _state.update { it.copy(plateNumber = value.uppercase(), error = null) }
    fun onOdometerChange(value: String) = _state.update { it.copy(odometer = value.filter(Char::isDigit), error = null) }

    fun save() {
        val s = _state.value
        val year = s.year.toIntOrNull()
        val odo = s.odometer.toIntOrNull()
        val maxYear = Year.now().value + 1

        val error = when {
            s.brand.isBlank() -> "Merek wajib diisi"
            s.model.isBlank() -> "Model wajib diisi"
            year == null || year !in 1980..maxYear -> "Tahun tidak valid"
            s.plateNumber.isBlank() -> "Plat nomor wajib diisi"
            odo == null -> "Odometer wajib diisi"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            val base = original
            if (base == null) {
                repo.addVehicle(
                    Vehicle(
                        name = s.name.trim(),
                        brand = s.brand.trim(),
                        model = s.model.trim(),
                        year = year!!,
                        plateNumber = s.plateNumber.trim(),
                        currentOdometer = odo!!
                    )
                )
            } else {
                repo.updateVehicle(
                    base.copy(
                        name = s.name.trim(),
                        brand = s.brand.trim(),
                        model = s.model.trim(),
                        year = year!!,
                        plateNumber = s.plateNumber.trim(),
                        currentOdometer = odo!!
                    )
                )
            }
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { VehicleFormViewModel(repo, vehicleId) } }
    }
}