package com.ian.garagejournal.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderNotifier
import com.ian.garagejournal.domain.ReminderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val DAY = 24L * 60 * 60 * 1000

@RunWith(RobolectricTestRunner::class)
class PeriodicCheckTest {

    private class FakeNotifier : ReminderNotifier {
        val delivered = mutableListOf<Pair<Long, ReminderLevel>>()
        val staleDays = mutableListOf<Int>()

        override fun show(reminder: Reminder, vehicle: Vehicle, status: ReminderStatus): Boolean {
            delivered += reminder.id to status.level
            return true
        }

        override fun cancel(reminderId: Long) {}

        override fun showStaleOdometer(vehicle: Vehicle, daysSinceUpdate: Int): Boolean {
            staleDays += daysSinceUpdate
            return true
        }
    }

    private lateinit var db: GarageDatabase
    private lateinit var repo: GarageRepository
    private lateinit var notifier: FakeNotifier

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        notifier = FakeNotifier()
        repo = GarageRepository(
            db.vehicleDao(), db.serviceLogDao(), db.fuelLogDao(), ReceiptStorage(context), db, notifier
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newVehicle(odometer: Int) = repo.addVehicle(
        Vehicle(
            name = "Test", brand = "Honda", model = "Beat", year = 2020,
            plateNumber = "BK 1234 XX", currentOdometer = odometer
        )
    )

    private fun fuel(vehicleId: Long, odo: Int, date: Long) = FuelLog(
        vehicleId = vehicleId, date = date, odometer = odo, liters = 3.0,
        pricePerLiter = 10_000, totalCost = 30_000, isFullTank = true
    )

    // interval 2.000 km, jadi jatuh tempo di lastOdo + 2.000
    private fun reminder(vehicleId: Long, lastOdo: Int) = Reminder(
        vehicleId = vehicleId, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
        lastDoneOdometer = lastOdo, lastDoneDate = System.currentTimeMillis()
    )

    @Test
    fun `jatuh tempo diulang sesuai jadwal lalu berhenti di batas`() = runBlocking<Unit> {
        val base = System.currentTimeMillis()
        val v = newVehicle(10_000)
        repo.addReminder(reminder(v, lastOdo = 8_000)) // langsung jatuh tempo: notifikasi pertama
        assertEquals(1, notifier.delivered.size)

        for (d in 1..60) repo.runPeriodicCheck(base + d * DAY + 60_000)

        assertEquals(7, notifier.delivered.size) // 1 awal + 6 pengulangan (hari ke-2, 5, 10, 17, 24, 31)
    }

    @Test
    fun `pengulangan menunggu jedanya`() = runBlocking<Unit> {
        val base = System.currentTimeMillis()
        val v = newVehicle(10_000)
        repo.addReminder(reminder(v, lastOdo = 8_000))

        repo.runPeriodicCheck(base + DAY)
        assertEquals(1, notifier.delivered.size)

        repo.runPeriodicCheck(base + 2 * DAY + 60_000)
        assertEquals(2, notifier.delivered.size)
    }

    @Test
    fun `tunda menahan pengulangan lalu berlanjut setelah lewat`() = runBlocking<Unit> {
        val base = System.currentTimeMillis()
        val v = newVehicle(10_000)
        val rid = repo.addReminder(reminder(v, lastOdo = 8_000))
        repo.snoozeReminder(rid, base + 5 * DAY)

        repo.runPeriodicCheck(base + 3 * DAY)
        assertEquals(1, notifier.delivered.size)

        repo.runPeriodicCheck(base + 5 * DAY + 60_000)
        assertEquals(2, notifier.delivered.size)
    }

    @Test
    fun `odometer basi diberitahu berjadwal berhenti di batas dan direset saat diperbarui`() = runBlocking<Unit> {
        val base = System.currentTimeMillis()
        val v = newVehicle(10_000)
        repo.addReminder(reminder(v, lastOdo = 9_500)) // masih aman, tapi motor punya pengingat

        repo.runPeriodicCheck(base + 29 * DAY)
        assertTrue(notifier.staleDays.isEmpty())

        repo.runPeriodicCheck(base + 31 * DAY)
        assertEquals(1, notifier.staleDays.size)

        repo.runPeriodicCheck(base + 32 * DAY) // belum 14 hari sejak yang pertama
        assertEquals(1, notifier.staleDays.size)

        repo.runPeriodicCheck(base + 46 * DAY)
        repo.runPeriodicCheck(base + 61 * DAY)
        assertEquals(3, notifier.staleDays.size)

        repo.runPeriodicCheck(base + 120 * DAY) // batas 3 kali tercapai
        assertEquals(3, notifier.staleDays.size)

        // Odometer diperbarui: hitungan di-reset
        repo.addFuelLog(fuel(v, 10_100, base))
        val updated = repo.observeVehicle(v).first()!!
        assertEquals(0, updated.staleNotifyCount)
        assertEquals(0L, updated.staleNotifiedAt)
    }

    @Test
    fun `motor tanpa pengingat tidak diganggu notifikasi odometer basi`() = runBlocking<Unit> {
        val base = System.currentTimeMillis()
        newVehicle(10_000)

        repo.runPeriodicCheck(base + 90 * DAY)

        assertTrue(notifier.staleDays.isEmpty())
    }

    @Test
    fun `perkiraan pemakaian memajukan status mendekati`() = runBlocking<Unit> {
        val now = System.currentTimeMillis()
        val v = newVehicle(10_000)
        repo.addFuelLog(fuel(v, 10_000, now - 10 * DAY))
        repo.addFuelLog(fuel(v, 11_000, now)) // 100 km per hari

        // jatuh tempo di 11.300: sisa 300 km (di atas ambang 200), tapi perkiraan habis dalam 3 hari
        val rid = repo.addReminder(reminder(v, lastOdo = 9_300))

        assertEquals(listOf(rid to ReminderLevel.DUE_SOON), notifier.delivered)
    }
}