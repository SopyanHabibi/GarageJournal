package com.ian.garagejournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Vehicle::class, ServiceLog::class, FuelLog::class, Reminder::class],
    version = 3,
    exportSchema = true
)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: GarageDatabase? = null

        fun get(context: Context): GarageDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GarageDatabase::class.java,
                    "garage_journal.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}