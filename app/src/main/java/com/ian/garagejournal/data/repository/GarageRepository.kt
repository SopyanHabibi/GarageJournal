package com.ian.garagejournal.data.repository

import androidx.room.withTransaction
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.FuelLogDao
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.ServiceLogDao
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.local.VehicleDao
import com.ian.garagejournal.domain.FuelCalculator
import com.ian.garagejournal.domain.ReminderCalculator
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderNotificationPolicy
import com.ian.garagejournal.domain.ReminderNotifier

class GarageRepository(
    private val vehicleDao: VehicleDao,
    private val serviceLogDao: ServiceLogDao,
    private val fuelLogDao: FuelLogDao,
    private val receiptStorage: ReceiptStorage,
    private val database: GarageDatabase,
    private val notifier: ReminderNotifier = ReminderNotifier.None

) {
    private val reminderDao by lazy { database.reminderDao() }
    // --- Vehicle ---
    fun observeVehicles() = vehicleDao.observeAll()
    fun observeVehicle(id: Long) = vehicleDao.observeById(id)
    suspend fun addVehicle(v: Vehicle) = vehicleDao.insert(v)
    suspend fun updateVehicle(v: Vehicle) {
        vehicleDao.update(v)
        checkReminders(v.id)
    }

    suspend fun deleteVehicle(v: Vehicle) {
        val receipts = serviceLogDao.getReceiptPaths(v.id)
        val reminderIds = reminderDao.getByVehicle(v.id).map { it.id }
        vehicleDao.delete(v)
        receipts.forEach { receiptStorage.delete(it) }
        reminderIds.forEach { notifier.cancel(it) }
    }

    // --- Service ---
    fun observeServiceLogs(vehicleId: Long) = serviceLogDao.observeByVehicle(vehicleId)
    fun observeServiceTotal(vehicleId: Long) = serviceLogDao.observeTotalCost(vehicleId)

    suspend fun addServiceLog(log: ServiceLog, resetReminderIds: Set<Long> = emptySet()) {
        database.withTransaction {
            serviceLogDao.insert(log)
            vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
            resetReminderIds.forEach {
                reminderDao.markDone(it, log.vehicleId, log.odometer, log.date)
            }
        }
        checkReminders(log.vehicleId)
    }
    suspend fun getServiceLog(id: Long) = serviceLogDao.getById(id)

    suspend fun updateServiceLog(log: ServiceLog) {
        serviceLogDao.update(log)
        vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
        checkReminders(log.vehicleId)
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
        checkReminders(log.vehicleId)
    }

    suspend fun updateFuelLog(log: FuelLog) {
        database.withTransaction {
            fuelLogDao.update(log.copy(kmPerLiter = null))
            recalculateEfficiency(log.vehicleId)
            vehicleDao.bumpOdometer(log.vehicleId, log.odometer)
        }
        checkReminders(log.vehicleId)
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

    // --- Reminder ---
    fun observeReminders(vehicleId: Long) = reminderDao.observeByVehicle(vehicleId)
    suspend fun getReminder(id: Long) = reminderDao.getById(id)

    suspend fun addReminder(r: Reminder): Long {
        val id = reminderDao.insert(r)
        checkReminders(r.vehicleId)
        return id
    }

    suspend fun updateReminder(r: Reminder) {
        reminderDao.update(r)
        checkReminders(r.vehicleId)
    }

    suspend fun deleteReminder(r: Reminder) {
        reminderDao.delete(r)
        notifier.cancel(r.id)
    }

    suspend fun markReminderDone(id: Long, vehicleId: Long, odometer: Int, date: Long) {
        reminderDao.markDone(id, vehicleId, odometer, date)
        checkReminders(vehicleId)
    }

    /** Dipakai tombol "Sudah diservis" di notifikasi: pakai odometer motor saat ini dan waktu sekarang. */
    suspend fun markReminderDoneNow(id: Long) {
        val r = reminderDao.getById(id) ?: return
        val v = vehicleDao.getById(r.vehicleId) ?: return
        markReminderDone(r.id, r.vehicleId, v.currentOdometer, System.currentTimeMillis())
    }

    suspend fun snoozeReminder(id: Long, until: Long) = reminderDao.setSnoozedUntil(id, until)

    /** Hitung ulang status semua pengingat motor ini; kirim notifikasi kalau levelnya naik. */
    suspend fun checkReminders(vehicleId: Long) {
        val vehicle = vehicleDao.getById(vehicleId) ?: return
        val now = System.currentTimeMillis()
        for (r in reminderDao.getByVehicle(vehicleId)) {
            val status = ReminderCalculator.status(r, vehicle.currentOdometer, now)
            val decision = ReminderNotificationPolicy.onDataChanged(r, status.level, now)
            // Gagal tampil (mis. izin ditolak): jangan dicatat sebagai terkirim, biar dicoba lagi nanti
            if (decision.shouldNotify && !notifier.show(decision.updated, vehicle, status)) continue
            if (decision.updated != r) reminderDao.update(decision.updated)
            if (status.level == ReminderLevel.OK) notifier.cancel(r.id)
        }
    }
}