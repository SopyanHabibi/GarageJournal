package com.ian.garagejournal.data.repository

import androidx.room.withTransaction
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.FuelLogDao
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.OdometerPoint
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.ServiceLogDao
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.local.VehicleDao
import com.ian.garagejournal.domain.FuelCalculator
import com.ian.garagejournal.domain.OdometerStalePolicy
import com.ian.garagejournal.domain.ReminderCalculator
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderNotificationPolicy
import com.ian.garagejournal.domain.ReminderNotifier
import com.ian.garagejournal.domain.UsageEstimator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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
        val old = vehicleDao.getById(v.id)
        val stamped = if (old != null && old.currentOdometer != v.currentOdometer) {
            v.copy(odometerUpdatedAt = System.currentTimeMillis(), staleNotifiedAt = 0L, staleNotifyCount = 0)
        } else v
        vehicleDao.update(stamped)
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
    /** Titik (tanggal, odometer) dari log BBM dan servis, untuk memperkirakan pemakaian harian. */
    fun observeUsagePoints(vehicleId: Long): Flow<List<OdometerPoint>> = combine(
        fuelLogDao.observeOdometerPoints(vehicleId),
        serviceLogDao.observeOdometerPoints(vehicleId)
    ) { fuel, service -> fuel + service }

    /** Dipanggil setelah data berubah (kamu sedang aktif di app). */
    suspend fun checkReminders(vehicleId: Long) {
        val vehicle = vehicleDao.getById(vehicleId) ?: return
        notifier.cancelStale(vehicle.id)
        val reminders = reminderDao.getByVehicle(vehicleId)
        if (reminders.isNotEmpty()) {
            evaluateReminders(vehicle, reminders, periodic = false, now = System.currentTimeMillis())
        }
    }

    /** Dipanggil pekerja latar belakang. `now` bisa disuntik supaya mudah dites. */
    suspend fun runPeriodicCheck(now: Long = System.currentTimeMillis()) {
        for (vehicle in vehicleDao.getAll()) {
            val reminders = reminderDao.getByVehicle(vehicle.id)
            if (reminders.isEmpty()) continue // motor tanpa pengingat tidak perlu diganggu
            evaluateReminders(vehicle, reminders, periodic = true, now = now)
            checkStaleOdometer(vehicle, now)
        }
    }

    private suspend fun evaluateReminders(
        vehicle: Vehicle,
        reminders: List<Reminder>,
        periodic: Boolean,
        now: Long
    ) {
        val avg = UsageEstimator.averageKmPerDay(observeUsagePoints(vehicle.id).first(), now)
        for (r in reminders) {
            val status = ReminderCalculator.status(r, vehicle.currentOdometer, now, avgKmPerDay = avg)
            val decision =
                if (periodic) ReminderNotificationPolicy.onPeriodicCheck(r, status.level, now)
                else ReminderNotificationPolicy.onDataChanged(r, status.level, now)
            // Gagal tampil (mis. izin ditolak): jangan dicatat sebagai terkirim, biar dicoba lagi nanti
            if (decision.shouldNotify && !notifier.show(decision.updated, vehicle, status)) continue
            if (decision.updated != r) reminderDao.update(decision.updated)
            if (status.level == ReminderLevel.OK) notifier.cancel(r.id)
        }
    }

    private suspend fun checkStaleOdometer(vehicle: Vehicle, now: Long) {
        if (!OdometerStalePolicy.shouldNotify(vehicle, now)) return
        val days = OdometerStalePolicy.daysSinceUpdate(vehicle, now)
        if (!notifier.showStaleOdometer(vehicle, days)) return
        vehicleDao.setStaleNotified(vehicle.id, now, vehicle.staleNotifyCount + 1)
    }
}