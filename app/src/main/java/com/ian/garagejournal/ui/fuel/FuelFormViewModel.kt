package com.ian.garagejournal.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class FuelFormState(
    val dateMillis: Long = System.currentTimeMillis(),
    val odometer: String = "",
    val liters: String = "",
    val pricePerLiter: String = "",
    val isFullTank: Boolean = true,
    val isEditing: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
) {
    val totalCost: Long?
        get() {
            val l = liters.toDoubleOrNull()
            val p = pricePerLiter.toLongOrNull()
            return if (l != null && p != null) (l * p).roundToLong() else null
        }
}

class FuelFormViewModel(
    private val repo: GarageRepository,
    private val vehicleId: Long,
    private val logId: Long // -1 = mode tambah
) : ViewModel() {

    private val _state = MutableStateFlow(FuelFormState(isEditing = logId != -1L))
    val state: StateFlow<FuelFormState> = _state.asStateFlow()

    private var original: FuelLog? = null

    init {
        viewModelScope.launch {
            if (logId != -1L) {
                repo.getFuelLog(logId)?.let { log ->
                    original = log
                    _state.update {
                        it.copy(
                            dateMillis = log.date,
                            odometer = log.odometer.toString(),
                            liters = log.liters.toString(),
                            pricePerLiter = log.pricePerLiter.toString(),
                            isFullTank = log.isFullTank
                        )
                    }
                }
            } else {
                // Mode tambah: isi odometer dari data motor, harga dari pengisian terakhir
                val vehicle = repo.observeVehicle(vehicleId).first()
                val lastPrice = repo.observeFuelLogs(vehicleId).first().firstOrNull()?.pricePerLiter
                _state.update {
                    it.copy(
                        odometer = vehicle?.currentOdometer?.toString() ?: "",
                        pricePerLiter = lastPrice?.toString() ?: ""
                    )
                }
            }
        }
    }

    fun onDateChange(millis: Long) = _state.update { it.copy(dateMillis = millis) }
    fun onOdometerChange(v: String) = _state.update { it.copy(odometer = v.filter(Char::isDigit), error = null) }
    fun onLitersChange(v: String) = _state.update { it.copy(liters = sanitizeDecimal(v), error = null) }
    fun onPriceChange(v: String) = _state.update { it.copy(pricePerLiter = v.filter(Char::isDigit), error = null) }
    fun onFullTankChange(v: Boolean) = _state.update { it.copy(isFullTank = v) }

    // Terima koma atau titik, maksimal satu pemisah desimal dan dua angka di belakangnya
    private fun sanitizeDecimal(input: String): String {
        val sb = StringBuilder()
        var dotSeen = false
        var decimals = 0
        for (c in input.replace(',', '.')) {
            when {
                c.isDigit() -> if (!dotSeen || decimals < 2) {
                    sb.append(c)
                    if (dotSeen) decimals++
                }
                c == '.' && !dotSeen -> {
                    sb.append(c)
                    dotSeen = true
                }
            }
        }
        return sb.toString()
    }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    fun save() {
        val s = _state.value
        val odo = s.odometer.toIntOrNull() ?: return fail("Odometer wajib diisi")
        val liters = s.liters.toDoubleOrNull()?.takeIf { it > 0 }
            ?: return fail("Jumlah liter harus lebih dari 0")
        val price = s.pricePerLiter.toLongOrNull()?.takeIf { it > 0 }
            ?: return fail("Harga per liter harus lebih dari 0")
        val total = s.totalCost ?: return fail("Total biaya tidak valid")

        viewModelScope.launch {
            val base = original
            val log = FuelLog(
                id = base?.id ?: 0,
                vehicleId = vehicleId,
                date = s.dateMillis,
                odometer = odo,
                liters = liters,
                pricePerLiter = price,
                totalCost = total,
                isFullTank = s.isFullTank,
                kmPerLiter = null // dihitung ulang oleh repository
            )
            if (base == null) repo.addFuelLog(log) else repo.updateFuelLog(log)
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun factory(repo: GarageRepository, vehicleId: Long, logId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { FuelFormViewModel(repo, vehicleId, logId) } }
    }
}