package com.ian.garagejournal

import android.app.Application
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.repository.GarageRepository

class GarageApp : Application() {
    private val database by lazy { GarageDatabase.get(this) }

    val repository by lazy {
        GarageRepository(
            database.vehicleDao(),
            database.serviceLogDao(),
            database.fuelLogDao()
        )
    }
}