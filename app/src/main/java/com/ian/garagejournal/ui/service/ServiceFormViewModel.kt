package com.ian.garagejournal.ui.service

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ServiceFormState(
    val title: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val odometer: String = "",
    val cost: String = "",
    val description: String = "",
    val receiptFileName: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val selectedReminderIds: Set<Long> = emptySet(),
    val isEditing: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class ServiceFormViewModel(
    private val repo: GarageRepository,
    private val storage: ReceiptStorage,
    private val vehicleId: Long,
    private val logId: Long // -1 = mode tambah
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceFormState(isEditing = logId != -1L))
    val state: StateFlow<ServiceFormState> = _state.asStateFlow()

    private var original: ServiceLog? = null
    private val addedFiles = mutableSetOf<String>() // file baru yang dibuat di sesi form ini
    private var saved = false

    init {
        viewModelScope.launch {
            if (logId != -1L) {
                repo.getServiceLog(logId)?.let { log ->
                    original = log
                    _state.update {
                        it.copy(
                            title = log.title,
                            dateMillis = log.date,
                            odometer = log.odometer.toString(),
                            cost = log.cost.toString(),
                            description = log.description,
                            receiptFileName = log.receiptPath
                        )
                    }
                }
            } else {
                // Mode tambah: isi odometer dengan odometer motor saat ini
                repo.observeVehicle(vehicleId).first()?.let { v ->
                    _state.update { it.copy(odometer = v.currentOdometer.toString()) }
                    val reminders = repo.observeReminders(vehicleId).first()
                    _state.update { it.copy(reminders = reminders) }
                }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, error = null) }
    fun onDateChange(millis: Long) = _state.update { it.copy(dateMillis = millis) }
    fun onOdometerChange(v: String) = _state.update { it.copy(odometer = v.filter(Char::isDigit), error = null) }
    fun onCostChange(v: String) = _state.update { it.copy(cost = v.filter(Char::isDigit), error = null) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onReminderToggle(id: Long) = _state.update {
        val next = if (id in it.selectedReminderIds) it.selectedReminderIds - id else it.selectedReminderIds + id
        it.copy(selectedReminderIds = next)
    }

    // --- Nota ---
    fun receiptFile(fileName: String): File = storage.resolve(fileName)
    fun prepareCameraFile(): String = storage.prepareCameraFile()
    fun cameraUri(fileName: String): Uri = storage.uriFor(fileName)

    fun onCameraResult(fileName: String, success: Boolean) {
        if (success) setReceipt(fileName) else storage.delete(fileName)
    }

    fun onGalleryPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching { storage.import(uri) }
                .onSuccess { setReceipt(it) }
                .onFailure { _state.update { s -> s.copy(error = "Gagal membaca gambar") } }
        }
    }

    private fun setReceipt(fileName: String) {
        discardIfAdded(_state.value.receiptFileName)
        addedFiles.add(fileName)
        _state.update { it.copy(receiptFileName = fileName, error = null) }
    }

    fun removeReceipt() {
        discardIfAdded(_state.value.receiptFileName)
        _state.update { it.copy(receiptFileName = null) }
    }

    // Hanya file yang dibuat di sesi ini yang boleh dihapus langsung.
    // Nota lama dari database baru dihapus setelah Simpan.
    private fun discardIfAdded(fileName: String?) {
        if (fileName != null && addedFiles.remove(fileName)) storage.delete(fileName)
    }

    fun save() {
        val s = _state.value
        val odo = s.odometer.toIntOrNull()
        val cost = s.cost.toLongOrNull()

        val error = when {
            s.title.isBlank() -> "Judul servis wajib diisi"
            odo == null -> "Odometer wajib diisi"
            cost == null -> "Biaya wajib diisi (isi 0 kalau gratis)"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            val base = original
            val log = ServiceLog(
                id = base?.id ?: 0,
                vehicleId = vehicleId,
                date = s.dateMillis,
                odometer = odo!!,
                title = s.title.trim(),
                description = s.description.trim(),
                cost = cost!!,
                receiptPath = s.receiptFileName
            )
            if (base == null) repo.addServiceLog(log, s.selectedReminderIds) else repo.updateServiceLog(log)

            // Nota lama yang diganti/dihapus -> bersihkan filenya
            if (base?.receiptPath != null && base.receiptPath != s.receiptFileName) {
                storage.delete(base.receiptPath)
            }
            saved = true
            _state.update { it.copy(saved = true) }
        }
    }

    override fun onCleared() {
        // Keluar tanpa menyimpan: buang foto yang sempat diambil
        if (!saved) addedFiles.forEach { storage.delete(it) }
    }

    companion object {
        fun factory(
            repo: GarageRepository,
            storage: ReceiptStorage,
            vehicleId: Long,
            logId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ServiceFormViewModel(repo, storage, vehicleId, logId) }
        }
    }
}