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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MANIFEST = "app=Garage Journal\nformat=1\n"
private const val VEHICLES =
    "id,name,brand,model,year,plateNumber,currentOdometer,createdAt,odometerUpdatedAt\r\n" +
            "1,Si Hitam,Honda,Beat,2020,BK 1 XX,10000,0,0\r\n"
private const val SERVICE_HEADER =
    "id,vehicleId,vehicle,date,dateReadable,odometer,title,description,cost,receiptFile\r\n"

@RunWith(RobolectricTestRunner::class)
class BackupImporterTest {

    private lateinit var context: Context
    private lateinit var source: GarageDatabase
    private lateinit var target: GarageDatabase
    private lateinit var storage: ReceiptStorage
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries().build()
        target = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries().build()
        storage = ReceiptStorage(context)
        importer = BackupImporter(target, storage)
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun receiptFiles(): Set<String> =
        File(context.filesDir, "receipts").list()?.toSet() ?: emptySet()

    /** Mengisi database sumber; mengembalikan nama file nota. */
    private suspend fun seedSource(): String {
        val vid = source.vehicleDao().insert(
            Vehicle(
                name = "Si Hitam", brand = "Honda", model = "Beat", year = 2020,
                plateNumber = "BK 1234 XX", currentOdometer = 10_150
            )
        )
        val receipt = storage.prepareCameraFile()
        storage.resolve(receipt).writeBytes(byteArrayOf(9, 8, 7))
        source.serviceLogDao().insert(
            ServiceLog(
                vehicleId = vid, date = 1_000L, odometer = 9_500, title = "Ganti oli",
                description = "Pakai oli, merek \"X\"\nDi bengkel A", cost = 50_000, receiptPath = receipt
            )
        )
        source.fuelLogDao().insert(
            FuelLog(
                vehicleId = vid, date = 2_000L, odometer = 10_000, liters = 4.0,
                pricePerLiter = 10_000, totalCost = 40_000, isFullTank = true
            )
        )
        source.fuelLogDao().insert(
            FuelLog(
                vehicleId = vid, date = 3_000L, odometer = 10_150, liters = 3.0,
                pricePerLiter = 10_000, totalCost = 30_000, isFullTank = true
            )
        )
        source.reminderDao().insert(
            Reminder(
                vehicleId = vid, name = "Ganti oli", intervalKm = 2_000, intervalMonths = 3,
                lastDoneOdometer = 9_500, lastDoneDate = 1_000L
            )
        )
        return receipt
    }

    private suspend fun exportSource(): ByteArray {
        val out = ByteArrayOutputStream()
        BackupExporter(source, storage).export(out)
        return out.toByteArray()
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, data) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `impor ke database kosong memulihkan semua data`() = runBlocking<Unit> {
        val originalReceipt = seedSource()
        val bytes = exportSource()

        val summary = importer.import(ByteArrayInputStream(bytes))

        assertEquals(ImportSummary(1, 0, 1, 2, 1, 1, 0), summary)

        val v = target.vehicleDao().getAll().single()
        assertEquals("Si Hitam", v.name)
        assertEquals(10_150, v.currentOdometer)

        val service = target.serviceLogDao().getAll().single()
        assertEquals(v.id, service.vehicleId)
        assertEquals("Pakai oli, merek \"X\"\nDi bengkel A", service.description)
        assertTrue(service.receiptPath != originalReceipt) // disimpan dengan nama baru
        assertArrayEquals(byteArrayOf(9, 8, 7), storage.resolve(service.receiptPath!!).readBytes())

        val fuel = target.fuelLogDao().getAll().sortedBy { it.odometer }
        assertNull(fuel[0].kmPerLiter)
        assertEquals(50.0, fuel[1].kmPerLiter!!, 0.001) // 150 km / 3 liter, dihitung ulang

        val reminder = target.reminderDao().getAll().single()
        assertEquals(v.id, reminder.vehicleId)
        assertEquals(2_000, reminder.intervalKm)
        assertEquals(3, reminder.intervalMonths)
    }

    @Test
    fun `impor dua kali tidak menggandakan data`() = runBlocking<Unit> {
        seedSource()
        val bytes = exportSource()
        importer.import(ByteArrayInputStream(bytes))
        val filesAfterFirst = receiptFiles()

        val second = importer.import(ByteArrayInputStream(bytes))

        assertEquals(ImportSummary(0, 1, 0, 0, 0, 0, 0), second)
        assertEquals(1, target.vehicleDao().getAll().size)
        assertEquals(1, target.serviceLogDao().getAll().size)
        assertEquals(filesAfterFirst, receiptFiles()) // salinan foto kedua dibuang
    }

    @Test
    fun `ID dipetakan ulang agar tidak bertabrakan dengan motor yang sudah ada`() = runBlocking<Unit> {
        seedSource()
        val bytes = exportSource()
        val existingId = target.vehicleDao().insert(
            Vehicle(
                name = "Motor Lain", brand = "Yamaha", model = "NMAX", year = 2022,
                plateNumber = "B 9 ZZ", currentOdometer = 500
            )
        )

        importer.import(ByteArrayInputStream(bytes))

        val imported = target.vehicleDao().getAll().first { it.plateNumber == "BK 1234 XX" }
        assertTrue(imported.id != existingId)
        assertEquals(imported.id, target.serviceLogDao().getAll().single().vehicleId)
        assertTrue(target.fuelLogDao().getAll().none { it.vehicleId == existingId })
    }

    @Test
    fun `motor dengan plat yang sama dilewati walau penulisannya beda`() = runBlocking<Unit> {
        seedSource()
        val bytes = exportSource()
        val filesBefore = receiptFiles()
        target.vehicleDao().insert(
            Vehicle(
                name = "Sudah Ada", brand = "Honda", model = "Beat", year = 2020,
                plateNumber = "bk1234xx", currentOdometer = 1
            )
        )

        val summary = importer.import(ByteArrayInputStream(bytes))

        assertEquals(0, summary.vehicles)
        assertEquals(1, summary.skippedVehicles)
        assertEquals(1, target.vehicleDao().getAll().size)
        assertTrue(target.serviceLogDao().getAll().isEmpty())
        assertEquals(filesBefore, receiptFiles()) // foto motor yang dilewati tidak tersisa
    }

    @Test
    fun `berkas yang bukan ZIP backup ditolak tanpa mengubah data`() = runBlocking<Unit> {
        val error = runCatching { importer.import(ByteArrayInputStream("bukan zip".toByteArray())) }
            .exceptionOrNull()

        assertTrue(error is BackupFormatException)
        assertTrue(target.vehicleDao().getAll().isEmpty())
    }

    @Test
    fun `data rusak membatalkan seluruh impor dan tidak meninggalkan foto`() = runBlocking<Unit> {
        val before = receiptFiles()
        val bytes = zipOf(
            "manifest.txt" to MANIFEST.toByteArray(),
            "vehicles.csv" to VEHICLES.toByteArray(),
            "service_logs.csv" to
                    (SERVICE_HEADER + "1,1,Si Hitam,0,1970-01-01,abc,Ganti oli,,50000,receipt_x.jpg\r\n").toByteArray(),
            "receipts/receipt_x.jpg" to byteArrayOf(1, 2, 3)
        )

        val error = runCatching { importer.import(ByteArrayInputStream(bytes)) }.exceptionOrNull()

        assertTrue(error is BackupFormatException)
        assertTrue(error!!.message!!.contains("service_logs.csv"))
        assertTrue(target.vehicleDao().getAll().isEmpty())
        assertEquals(before, receiptFiles())
    }

    @Test
    fun `foto nota yang tidak ada di dalam ZIP dilaporkan`() = runBlocking<Unit> {
        val bytes = zipOf(
            "manifest.txt" to MANIFEST.toByteArray(),
            "vehicles.csv" to VEHICLES.toByteArray(),
            "service_logs.csv" to
                    (SERVICE_HEADER + "1,1,Si Hitam,0,1970-01-01,9500,Ganti oli,,50000,receipt_x.jpg\r\n").toByteArray()
        )

        val summary = importer.import(ByteArrayInputStream(bytes))

        assertEquals(1, summary.serviceLogs)
        assertEquals(1, summary.missingReceipts)
        assertEquals(0, summary.receipts)
        assertNull(target.serviceLogDao().getAll().single().receiptPath)
    }
}