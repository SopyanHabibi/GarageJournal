package com.ian.garagejournal

import android.app.Application
import com.ian.garagejournal.data.backup.BackupExporter
import com.ian.garagejournal.data.backup.BackupImporter
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.notification.AndroidReminderNotifier
import com.ian.garagejournal.notification.ReminderScheduler

class GarageApp : Application() {
    private val database by lazy { GarageDatabase.get(this) }

    val receiptStorage by lazy { ReceiptStorage(this) }
    val reminderNotifier by lazy { AndroidReminderNotifier(this) }
    val backupExporter by lazy { BackupExporter(database, receiptStorage) }
    val backupImporter by lazy { BackupImporter(database, receiptStorage) }

    val repository by lazy {
        GarageRepository(
            database.vehicleDao(),
            database.serviceLogDao(),
            database.fuelLogDao(),
            receiptStorage,
            database,
            reminderNotifier
        )
    }

    override fun onCreate() {
        super.onCreate()
        reminderNotifier.createChannels()
        ReminderScheduler.schedulePeriodic(this)
    }
}