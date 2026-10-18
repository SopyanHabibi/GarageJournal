package com.ian.garagejournal.data.repository

import androidx.room.withTransaction
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.FuelLogDao
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.ServiceLogDao
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.local.VehicleDao
import com.ian.garagejournal.domain.FuelCalculator

class GarageRepository(
    private val vehicleDao: VehicleDao,
    private val serviceLogDao: ServiceLogDao,
    private val fuelLogDao: FuelLogDao,
    private val receiptStorage: ReceiptStorage,
    private val database: GarageDatabase
) {
    // --- Vehicle ---
    fun observeVehicles() = vehicleDao.observeAll()
    fun observeVehicle(id: Long) = vehicleDao.observeById(id)
    suspend fun addVehicle(v: Vehicle) = vehicleDao.insert(v)
    suspend fun updateVehicle(v: Vehicle) = vehicleDao.update(v)
    suspend fun deleteVehicle(v: Vehicle) {
        val receipts = serviceLogDao.getReceiptPaths(v.id) // ambil dulu sebelum barisnya hilang
        vehicleDao.delete(v)
        receipts.forEach { receiptStorage.delete(it) }
    }

    // --- Service ---
    fun observeServiceLogs(vehicleId: Long) = serviceLogDao.observeByVehicle(vehicleId)
    fun observeServiceTotal(vehicleId: Long) = serviceLogDao.observeTotalCost(vehicleId)

    suspend fun addServiceLog(log: ServiceLog) {
        serviceLogDao.insert(log)
        vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
    }
    suspend fun getServiceLog(id: Long) = serviceLogDao.getById(id)

    suspend fun updateServiceLog(log: ServiceLog) {
        serviceLogDao.update(log)
        vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
    }

    suspend fun deleteServiceLog(log: ServiceLog) {
        serviceLogDao.delete(log)
        receiptStorage.delete(log.receiptPath)
    }

    // --- Fuel ---
    fun observeFuelLogs(vehicleId: Long) = fuelLogDao.observeByVehicle(vehicleId)

    suspend fun getFuelLog(id: Long) = fuelLogDao.getById(id)

    suspend fun addFuelLog(log: FuelLog) {
        database.withTransaction {
            fuelLogDao.insert(log.copy(kmPerLiter = null))
            recalculateEfficiency(log.vehicleId)
            vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
        }
    }

    suspend fun updateFuelLog(log: FuelLog) {
        database.withTransaction {
            fuelLogDao.update(log.copy(kmPerLiter = null))
            recalculateEfficiency(log.vehicleId)
            vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
        }
    }

    suspend fun deleteFuelLog(log: FuelLog) {
        database.withTransaction {
            fuelLogDao.delete(log)
            recalculateEfficiency(log.vehicleId)
        }
    }

    // Hitung ulang KM/L untuk SEMUA log motor ini, simpan hanya yang berubah.
    private suspend fun recalculateEfficiency(vehicleId: Long) {
        val logs = fuelLogDao.getAllAscending(vehicleId)
        val results = FuelCalculator.efficiencies(logs)
        val changed = logs
            .filter { it.kmPerLiter != results[it.id] }
            .map { it.copy(kmPerLiter = results[it.id]) }
        if (changed.isNotEmpty()) fuelLogDao.updateAll(changed)
    }
}