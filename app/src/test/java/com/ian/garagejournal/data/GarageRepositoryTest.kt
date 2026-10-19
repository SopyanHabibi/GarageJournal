package com.ian.garagejournal.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GarageRepositoryTest {

    private lateinit var db: GarageDatabase
    private lateinit var storage: ReceiptStorage
    private lateinit var repo: GarageRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = ReceiptStorage(context)
        repo = GarageRepository(
            db.vehicleDao(), db.serviceLogDao(), db.fuelLogDao(), storage, db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- helper ---

    private suspend fun newVehicle(odometer: Int = 10_000): Long = repo.addVehicle(
        Vehicle(
            name = "Test", brand = "Honda", model = "Beat", year = 2020,
            plateNumber = "BK 1234 XX", currentOdometer = odometer
        )
    )

    private fun fuel(vehicleId: Long, odo: Int, liters: Double, full: Boolean = true) = FuelLog(
        vehicleId = vehicleId, date = odo.toLong(), odometer = odo, liters = liters,
        pricePerLiter = 10_000, totalCost = (liters * 10_000).toLong(), isFullTank = full
    )

    private fun service(vehicleId: Long, odo: Int, cost: Long, receipt: String? = null) = ServiceLog(
        vehicleId = vehicleId, date = 0L, odometer = odo, title = "Ganti oli",
        cost = cost, receiptPath = receipt
    )

    private suspend fun fuelLogsAscending(vehicleId: Long) =
        repo.observeFuelLogs(vehicleId).first().sortedBy { it.odometer }

    // --- BBM: hitung ulang KM per liter ---

    @Test
    fun `dua pengisian penuh menghasilkan 50 km per liter`() = runBlocking<Unit> {
        val v = newVehicle()
        repo.addFuelLog(fuel(v, 10_000, 4.0))
        repo.addFuelLog(fuel(v, 10_150, 3.0))

        val logs = fuelLogsAscending(v)
        assertNull(logs[0].kmPerLiter)
        assertEquals(50.0, logs[1].kmPerLiter!!, 0.001)
    }

    @Test
    fun `pengisian sebagian di antaranya menghitung ulang tanpa diedit`() = runBlocking<Unit> {
        val v = newVehicle()
        repo.addFuelLog(fuel(v, 10_000, 4.0))
        repo.addFuelLog(fuel(v, 10_150, 3.0))
        assertEquals(50.0, fuelLogsAscending(v)[1].kmPerLiter!!, 0.001)

        repo.addFuelLog(fuel(v, 10_050, 1.0, full = false))

        val logs = fuelLogsAscending(v) // 10.000, 10.050, 10.150
        assertNull(logs[1].kmPerLiter)
        assertEquals(37.5, logs[2].kmPerLiter!!, 0.001) // 150 km / (1 + 3) liter
    }

    @Test
    fun `hapus pengisian sebagian mengembalikan km per liter`() = runBlocking<Unit> {
        val v = newVehicle()
        repo.addFuelLog(fuel(v, 10_000, 4.0))
        repo.addFuelLog(fuel(v, 10_050, 1.0, full = false))
        repo.addFuelLog(fuel(v, 10_150, 3.0))
        assertEquals(37.5, fuelLogsAscending(v)[2].kmPerLiter!!, 0.001)

        repo.deleteFuelLog(fuelLogsAscending(v)[1])

        val logs = fuelLogsAscending(v)
        assertEquals(2, logs.size)
        assertEquals(50.0, logs[1].kmPerLiter!!, 0.001)
    }

    @Test
    fun `edit odometer menghitung ulang km per liter`() = runBlocking<Unit> {
        val v = newVehicle()
        repo.addFuelLog(fuel(v, 10_000, 4.0))
        repo.addFuelLog(fuel(v, 10_150, 3.0))

        repo.updateFuelLog(fuelLogsAscending(v)[1].copy(odometer = 10_300))

        assertEquals(100.0, fuelLogsAscending(v)[1].kmPerLiter!!, 0.001) // 300 km / 3 liter
    }

    // --- Servis ---

    @Test
    fun `total biaya servis nol jika kosong lalu dijumlahkan`() = runBlocking<Unit> {
        val v = newVehicle()
        assertEquals(0L, repo.observeServiceTotal(v).first())

        repo.addServiceLog(service(v, 10_100, 150_000))
        repo.addServiceLog(service(v, 10_200, 75_000))

        assertEquals(225_000L, repo.observeServiceTotal(v).first())
    }

    @Test
    fun `hapus log servis ikut menghapus file nota`() = runBlocking<Unit> {
        val v = newVehicle()
        val receipt = storage.prepareCameraFile()
        repo.addServiceLog(service(v, 10_100, 150_000, receipt))
        assertTrue(storage.resolve(receipt).exists())

        repo.deleteServiceLog(repo.observeServiceLogs(v).first().single())

        assertFalse(storage.resolve(receipt).exists())
    }

    // --- Motor ---

    @Test
    fun `odometer motor hanya naik tidak bisa turun`() = runBlocking<Unit> {
        val v = newVehicle(odometer = 10_000)

        repo.addServiceLog(service(v, 9_000, 50_000)) // input log lama
        assertEquals(10_000, repo.observeVehicle(v).first()!!.currentOdometer)

        repo.addServiceLog(service(v, 10_500, 50_000))
        assertEquals(10_500, repo.observeVehicle(v).first()!!.currentOdometer)
    }

    @Test
    fun `hapus motor ikut menghapus semua log dan file nota`() = runBlocking<Unit> {
        val v = newVehicle()
        val receipt = storage.prepareCameraFile()
        repo.addServiceLog(service(v, 10_100, 150_000, receipt))
        repo.addFuelLog(fuel(v, 10_100, 3.0))
        assertTrue(storage.resolve(receipt).exists())

        repo.deleteVehicle(repo.observeVehicle(v).first()!!)

        assertTrue(repo.observeServiceLogs(v).first().isEmpty())
        assertTrue(repo.observeFuelLogs(v).first().isEmpty())
        assertFalse(storage.resolve(receipt).exists())
    }
}