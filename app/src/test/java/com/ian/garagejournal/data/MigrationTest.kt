package com.ian.garagejournal.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.MIGRATION_1_2
import com.ian.garagejournal.data.local.MIGRATION_2_3
import com.ian.garagejournal.data.local.MIGRATION_3_4
import com.ian.garagejournal.data.local.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun createVersion1Tables(db: SQLiteDatabase) {
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

        db.execSQL("INSERT INTO vehicles VALUES (1, 'Si Hitam', 'Honda', 'Beat', 2020, 'BK 1234 XX', 10000, 0)")
        db.execSQL("INSERT INTO service_logs VALUES (1, 1, 0, 9500, 'Ganti oli', '', 50000, NULL)")
        db.execSQL("INSERT INTO fuel_logs VALUES (1, 1, 0, 9800, 3.5, 10000, 35000, 1, NULL)")
    }

    private fun createVersion1Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        createVersion1Tables(db)
        db.version = 1
        db.close()
    }

    private fun createVersion2Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        createVersion1Tables(db)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, `name` TEXT NOT NULL, `intervalKm` INTEGER, `intervalMonths` INTEGER, " +
                    "`lastDoneOdometer` INTEGER NOT NULL, `lastDoneDate` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_vehicleId` ON `reminders` (`vehicleId`)")
        db.execSQL(
            "INSERT INTO reminders (id, vehicleId, name, intervalKm, intervalMonths, lastDoneOdometer, lastDoneDate) " +
                    "VALUES (1, 1, 'Ganti oli', 2000, NULL, 9500, 0)"
        )
        db.version = 2
        db.close()
    }

    private fun createVersion3Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        createVersion1Tables(db)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, `name` TEXT NOT NULL, `intervalKm` INTEGER, `intervalMonths` INTEGER, " +
                    "`lastDoneOdometer` INTEGER NOT NULL, `lastDoneDate` INTEGER NOT NULL, " +
                    "`lastNotifiedLevel` INTEGER NOT NULL DEFAULT 0, `lastNotifiedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "`notifyCount` INTEGER NOT NULL DEFAULT 0, `snoozedUntil` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_vehicleId` ON `reminders` (`vehicleId`)")
        db.version = 3
        db.close()
    }

    private fun openLatest() = Room.databaseBuilder(context, GarageDatabase::class.java, dbName)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()

    @Test
    fun `migrasi dari versi 1 sampai 3 mempertahankan data lama`() = runBlocking<Unit> {
        createVersion1Database()
        val db = openLatest()
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
            val saved = db.reminderDao().getById(id)!!
            assertEquals("Ganti oli", saved.name)
            assertEquals(0, saved.lastNotifiedLevel)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migrasi 2 ke 3 mempertahankan pengingat dan mengisi kolom notifikasi`() = runBlocking<Unit> {
        createVersion2Database()
        val db = openLatest()
        try {
            val r = db.reminderDao().getById(1)!!
            assertEquals("Ganti oli", r.name)
            assertEquals(9_500, r.lastDoneOdometer)
            assertEquals(0, r.lastNotifiedLevel)
            assertEquals(0, r.notifyCount)
            assertEquals(0L, r.snoozedUntil)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migrasi 3 ke 4 memberi masa tenggang odometer pada motor lama`() = runBlocking<Unit> {
        val before = System.currentTimeMillis()
        createVersion3Database()
        val db = openLatest()
        try {
            val v = db.vehicleDao().getById(1)!!
            assertEquals("Si Hitam", v.name)
            assertTrue(v.odometerUpdatedAt >= before)
            assertEquals(0, v.staleNotifyCount)
        } finally {
            db.close()
        }
    }
}