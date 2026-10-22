package com.ian.garagejournal.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class BackupExporterTest {

    private lateinit var db: GarageDatabase
    private lateinit var storage: ReceiptStorage
    private lateinit var exporter: BackupExporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = ReceiptStorage(context)
        exporter = BackupExporter(db, storage)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private suspend fun insertVehicle(): Long = db.vehicleDao().insert(
        Vehicle(
            name = "Si Hitam", brand = "Honda", model = "Beat", year = 2020,
            plateNumber = "BK 1 XX", currentOdometer = 10_000
        )
    )

    private suspend fun insertService(vehicleId: Long, description: String, receipt: String?) =
        db.serviceLogDao().insert(
            ServiceLog(
                vehicleId = vehicleId, date = 0L, odometer = 9_500, title = "Ganti oli",
                description = description, cost = 50_000, receiptPath = receipt
            )
        )

    @Test
    fun `ekspor menghasilkan semua berkas dan foto nota`() = runBlocking<Unit> {
        val vid = insertVehicle()
        val receipt = storage.prepareCameraFile()
        storage.resolve(receipt).writeBytes(byteArrayOf(1, 2, 3))
        insertService(vid, "Pakai oli, merek \"X\"\nDi bengkel A", receipt)
        db.fuelLogDao().insert(
            FuelLog(
                vehicleId = vid, date = 0L, odometer = 9_800, liters = 3.5,
                pricePerLiter = 10_000, totalCost = 35_000, isFullTank = true
            )
        )
        db.reminderDao().insert(
            Reminder(
                vehicleId = vid, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
                lastDoneOdometer = 9_500, lastDoneDate = 0L
            )
        )

        val out = ByteArrayOutputStream()
        val summary = exporter.export(out)
        val files = unzip(out.toByteArray())

        assertEquals(
            setOf(
                "manifest.txt", "vehicles.csv", "service_logs.csv",
                "fuel_logs.csv", "reminders.csv", "receipts/$receipt"
            ),
            files.keys
        )
        assertEquals(ExportSummary(1, 1, 1, 1, 1, 0), summary)
        assertArrayEquals(byteArrayOf(1, 2, 3), files["receipts/$receipt"])

        val vehicles = files["vehicles.csv"]!!.toString(Charsets.UTF_8).trimEnd().lines()
        assertEquals(
            "id,name,brand,model,year,plateNumber,currentOdometer,createdAt,odometerUpdatedAt",
            vehicles[0]
        )
        assertTrue(vehicles[1].startsWith("1,Si Hitam,Honda,Beat,2020,BK 1 XX,10000,"))

        // Koma, kutip, dan baris baru di catatan tidak boleh merusak kolom
        val services = files["service_logs.csv"]!!.toString(Charsets.UTF_8)
        assertTrue(services.contains("\"Pakai oli, merek \"\"X\"\"\nDi bengkel A\""))

        val fuel = files["fuel_logs.csv"]!!.toString(Charsets.UTF_8)
        assertTrue(fuel.contains(",3.5,10000,35000,true,")) // desimal pakai titik, bukan koma
    }

    @Test
    fun `foto nota yang filenya hilang dihitung dan dilewati`() = runBlocking<Unit> {
        val vid = insertVehicle()
        val receipt = storage.prepareCameraFile()
        storage.delete(receipt) // file hilang dari penyimpanan
        insertService(vid, "", receipt)

        val out = ByteArrayOutputStream()
        val summary = exporter.export(out)
        val files = unzip(out.toByteArray())

        assertEquals(0, summary.receipts)
        assertEquals(1, summary.missingReceipts)
        assertTrue(files.keys.none { it.startsWith("receipts/") })
    }

    @Test
    fun `database kosong tetap menghasilkan berkas dengan header saja`() = runBlocking<Unit> {
        val out = ByteArrayOutputStream()
        val summary = exporter.export(out)
        val files = unzip(out.toByteArray())

        assertEquals(ExportSummary(0, 0, 0, 0, 0, 0), summary)
        assertEquals(1, files["vehicles.csv"]!!.toString(Charsets.UTF_8).trimEnd().lines().size)
    }
}