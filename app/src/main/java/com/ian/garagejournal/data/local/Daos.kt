package com.ian.garagejournal.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<Vehicle?>

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    // Odometer hanya naik, tidak boleh turun karena input log lama
    @Query("UPDATE vehicles SET currentOdometer = :odo WHERE id = :id AND currentOdometer < :odo")
    suspend fun bumpOdometer(id: Long, odo: Int)
}

@Dao
interface ServiceLogDao {
    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<ServiceLog>>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM service_logs WHERE vehicleId = :vehicleId")
    fun observeTotalCost(vehicleId: Long): Flow<Long>

    @Query("SELECT * FROM service_logs WHERE id = :id")
    suspend fun getById(id: Long): ServiceLog?

    @Query("SELECT receiptPath FROM service_logs WHERE vehicleId = :vehicleId AND receiptPath IS NOT NULL")
    suspend fun getReceiptPaths(vehicleId: Long): List<String>

    @Insert
    suspend fun insert(log: ServiceLog): Long

    @Update
    suspend fun update(log: ServiceLog)

    @Delete
    suspend fun delete(log: ServiceLog)
}

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT AVG(kmPerLiter) FROM fuel_logs WHERE vehicleId = :vehicleId AND kmPerLiter IS NOT NULL")
    fun observeAverageEfficiency(vehicleId: Long): Flow<Double?>

    // Isi tangki penuh terakhir sebelum odometer tertentu
    @Query("""
        SELECT * FROM fuel_logs
        WHERE vehicleId = :vehicleId AND isFullTank = 1 AND odometer < :odometer
        ORDER BY odometer DESC LIMIT 1
    """)
    suspend fun getPreviousFullTank(vehicleId: Long, odometer: Int): FuelLog?

    // Total liter pengisian parsial di antara dua titik odometer
    @Query("""
        SELECT COALESCE(SUM(liters), 0) FROM fuel_logs
        WHERE vehicleId = :vehicleId AND odometer > :fromOdo AND odometer < :toOdo
    """)
    suspend fun sumLitersBetween(vehicleId: Long, fromOdo: Int, toOdo: Int): Double

    @Insert
    suspend fun insert(log: FuelLog): Long

    @Delete
    suspend fun delete(log: FuelLog)
}