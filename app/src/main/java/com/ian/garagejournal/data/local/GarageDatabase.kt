package com.ian.garagejournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE fuel_logs ADD COLUMN fuelTypeName TEXT NOT NULL DEFAULT ''"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE service_logs ADD COLUMN category TEXT NOT NULL DEFAULT 'LAINNYA'"
        )
    }
}

@Database(
    entities = [Vehicle::class, ServiceLog::class, FuelLog::class, Reminder::class],
    version = 6,
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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build().also { INSTANCE = it }
            }
    }
}