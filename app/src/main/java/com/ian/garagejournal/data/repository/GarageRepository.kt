package com.ian.garagejournal.data.repository

import com.ian.garagejournal.data.local.*

class GarageRepository(
    private val vehicleDao: VehicleDao,
    private val serviceLogDao: ServiceLogDao,
    private val fuelLogDao: FuelLogDao
) {
    // --- Vehicle ---
    fun observeVehicles() = vehicleDao.observeAll()
    fun observeVehicle(id: Long) = vehicleDao.observeById(id)
    suspend fun addVehicle(v: Vehicle) = vehicleDao.insert(v)
    suspend fun updateVehicle(v: Vehicle) = vehicleDao.update(v)
    suspend fun deleteVehicle(v: Vehicle) = vehicleDao.delete(v)

    // --- Service ---
    fun observeServiceLogs(vehicleId: Long) = serviceLogDao.observeByVehicle(vehicleId)
    fun observeServiceTotal(vehicleId: Long) = serviceLogDao.observeTotalCost(vehicleId)

    suspend fun addServiceLog(log: ServiceLog) {
        serviceLogDao.insert(log)
        vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
    }
    suspend fun updateServiceLog(log: ServiceLog) = serviceLogDao.update(log)
    suspend fun deleteServiceLog(log: ServiceLog) = serviceLogDao.delete(log)

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