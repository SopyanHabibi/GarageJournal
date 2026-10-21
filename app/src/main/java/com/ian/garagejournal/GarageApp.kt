package com.ian.garagejournal

import android.app.Application
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.notification.AndroidReminderNotifier

class GarageApp : Application() {
    private val database by lazy { GarageDatabase.get(this) }

    val receiptStorage by lazy { ReceiptStorage(this) }
    val reminderNotifier by lazy { AndroidReminderNotifier(this) }

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
    }
}