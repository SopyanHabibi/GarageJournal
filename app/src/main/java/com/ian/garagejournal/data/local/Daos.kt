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

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?
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
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC, date DESC, id DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer ASC, date ASC, id ASC")
    suspend fun getAllAscending(vehicleId: Long): List<FuelLog>

    @Query("SELECT * FROM fuel_logs WHERE id = :id")
    suspend fun getById(id: Long): FuelLog?

    @Insert
    suspend fun insert(log: FuelLog): Long

    @Update
    suspend fun update(log: FuelLog)

    @Update
    suspend fun updateAll(logs: List<FuelLog>)

    @Delete
    suspend fun delete(log: FuelLog)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY name ASC")
    fun observeByVehicle(vehicleId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    // Syarat odometer: servis lama yang diinput belakangan tidak boleh memundurkan pengingat
    @Query(
        """
        UPDATE reminders SET lastDoneOdometer = :odometer, lastDoneDate = :date
        WHERE id = :id AND vehicleId = :vehicleId AND lastDoneOdometer <= :odometer
        """
    )
    suspend fun markDone(id: Long, vehicleId: Long, odometer: Int, date: Long)

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId")
    suspend fun getByVehicle(vehicleId: Long): List<Reminder>

    @Query("UPDATE reminders SET snoozedUntil = :until WHERE id = :id")
    suspend fun setSnoozedUntil(id: Long, until: Long)
}