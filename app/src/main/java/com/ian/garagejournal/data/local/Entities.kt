package com.ian.garagejournal.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // nama panggilan, mis. "Si Hitam"
    val brand: String,
    val model: String,
    val year: Int,
    val plateNumber: String,
    val currentOdometer: Int,
    val createdAt: Long = System.currentTimeMillis(),
    // --- pelacakan odometer basi ---
    @ColumnInfo(defaultValue = "0") val odometerUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val staleNotifiedAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val staleNotifyCount: Int = 0
)

@Entity(
    tableName = "service_logs",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class ServiceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,                // epoch millis
    val odometer: Int,
    val title: String,             // mis. "Ganti oli"
    val description: String = "",
    val cost: Long,                // rupiah, pakai Long biar aman
    val receiptPath: String? = null // nama file nota di folder receipts
)

@Entity(
    tableName = "fuel_logs",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val odometer: Int,
    val liters: Double,
    val pricePerLiter: Long,
    val totalCost: Long,
    val isFullTank: Boolean,       // penting buat akurasi KM/L
    val kmPerLiter: Double? = null // dihitung ulang otomatis oleh repository, jangan diisi manual
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastDoneOdometer: Int,
    val lastDoneDate: Long,
    // --- status notifikasi ---
    @ColumnInfo(defaultValue = "0") val lastNotifiedLevel: Int = 0, // kode ReminderLevel terakhir yang sudah dinotifikasi
    @ColumnInfo(defaultValue = "0") val lastNotifiedAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val notifyCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val snoozedUntil: Long = 0
)