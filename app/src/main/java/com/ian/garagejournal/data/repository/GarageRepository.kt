package com.ian.garagejournal.data.repository

import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.FuelLogDao
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.ServiceLogDao
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.local.VehicleDao

class GarageRepository(
    private val vehicleDao: VehicleDao,
    private val serviceLogDao: ServiceLogDao,
    private val fuelLogDao: FuelLogDao,
    private val receiptStorage: ReceiptStorage
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
    fun observeAverageEfficiency(vehicleId: Long) = fuelLogDao.observeAverageEfficiency(vehicleId)

    suspend fun addFuelLog(log: FuelLog) {
        var efficiency: Double? = null

        if (log.isFullTank) {
            val prevFull = fuelLogDao.getPreviousFullTank(log.vehicleId, log.odometer)
            if (prevFull != null) {
                val distance = log.odometer - prevFull.odometer
                val litersUsed = fuelLogDao.sumLitersBetween(
                    log.vehicleId, prevFull.odometer, log.odometer
                ) + log.liters
                if (distance > 0 && litersUsed > 0) {
                    efficiency = distance / litersUsed
                }
            }
        }

        fuelLogDao.insert(log.copy(kmPerLiter = efficiency))
        vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
    }
    suspend fun deleteFuelLog(log: FuelLog) = fuelLogDao.delete(log)
}