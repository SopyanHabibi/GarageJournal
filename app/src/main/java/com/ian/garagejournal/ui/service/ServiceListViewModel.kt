package com.ian.garagejournal.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ServiceListViewModel(
    private val repo: GarageRepository,
    private val storage: ReceiptStorage,
    vehicleId: Long
) : ViewModel() {

    val logs: StateFlow<List<ServiceLog>> = repo.observeServiceLogs(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCost: StateFlow<Long> = repo.observeServiceTotal(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun receiptFile(fileName: String): File = storage.resolve(fileName)

    fun delete(log: ServiceLog) {
        viewModelScope.launch { repo.deleteServiceLog(log) }
    }

    companion object {
        fun factory(
            repo: GarageRepository,
            storage: ReceiptStorage,
            vehicleId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ServiceListViewModel(repo, storage, vehicleId) }
        }
    }
}