package com.ian.garagejournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.FuelLogDao
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.ServiceLogDao
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.local.VehicleDao

@Database(
    entities = [Vehicle::class, ServiceLog::class, FuelLog::class],
    version = 1,
    exportSchema = true
)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun fuelLogDao(): FuelLogDao

    companion object {
        @Volatile private var INSTANCE: GarageDatabase? = null

        fun get(context: Context): GarageDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GarageDatabase::class.java,
                    "garage_journal.db"
                ).build().also { INSTANCE = it }
            }
    }
}