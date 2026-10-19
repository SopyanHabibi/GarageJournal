package com.ian.garagejournal.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.MIGRATION_1_2
import com.ian.garagejournal.data.local.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion1Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `brand` TEXT NOT NULL, `model` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                    "`plateNumber` TEXT NOT NULL, `currentOdometer` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `service_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `odometer` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, `description` TEXT NOT NULL, `cost` INTEGER NOT NULL, `receiptPath` TEXT, " +
                    "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_logs_vehicleId` ON `service_logs` (`vehicleId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fuel_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `odometer` INTEGER NOT NULL, " +
                    "`liters` REAL NOT NULL, `pricePerLiter` INTEGER NOT NULL, `totalCost` INTEGER NOT NULL, " +
                    "`isFullTank` INTEGER NOT NULL, `kmPerLiter` REAL, " +
                    "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_vehicleId` ON `fuel_logs` (`vehicleId`)")

        db.execSQL(
            "INSERT INTO vehicles VALUES (1, 'Si Hitam', 'Honda', 'Beat', 2020, 'BK 1234 XX', 10000, 0)"
        )
        db.execSQL(
            "INSERT INTO service_logs VALUES (1, 1, 0, 9500, 'Ganti oli', '', 50000, NULL)"
        )
        db.execSQL(
            "INSERT INTO fuel_logs VALUES (1, 1, 0, 9800, 3.5, 10000, 35000, 1, NULL)"
        )
        db.version = 1
        db.close()
    }

    @Test
    fun `migrasi 1 ke 2 mempertahankan data lama dan menambah tabel reminders`() = runBlocking<Unit> {
        createVersion1Database()

        val db = Room.databaseBuilder(context, GarageDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            assertEquals("Si Hitam", db.vehicleDao().observeAll().first().single().name)
            assertEquals(1, db.serviceLogDao().observeByVehicle(1).first().size)
            assertEquals(1, db.fuelLogDao().observeByVehicle(1).first().size)

            val id = db.reminderDao().insert(
                Reminder(
                    vehicleId = 1, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
                    lastDoneOdometer = 9_500, lastDoneDate = 0L
                )
            )
            assertEquals("Ganti oli", db.reminderDao().getById(id)!!.name)
        } finally {
            db.close()
        }
    }
}