package com.ian.garagejournal.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderNotifier
import com.ian.garagejournal.domain.ReminderStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderNotificationTest {

    private class FakeNotifier : ReminderNotifier {
        var canShow = true
        val delivered = mutableListOf<Pair<Long, ReminderLevel>>()
        val cancelled = mutableListOf<Long>()

        override fun show(reminder: Reminder, vehicle: Vehicle, status: ReminderStatus): Boolean {
            if (!canShow) return false
            delivered += reminder.id to status.level
            return true
        }

        override fun cancel(reminderId: Long) {
            cancelled += reminderId
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

    private fun fuel(vehicleId: Long, odo: Int) = FuelLog(
        vehicleId = vehicleId, date = odo.toLong(), odometer = odo, liters = 3.0,
        pricePerLiter = 10_000, totalCost = 30_000, isFullTank = true
    )

    private fun service(vehicleId: Long, odo: Int) = ServiceLog(
        vehicleId = vehicleId, date = 0L, odometer = odo, title = "Ganti oli", cost = 50_000
    )

    // interval 2.000 km, jadi jatuh tempo di lastOdo + 2.000
    private fun reminder(vehicleId: Long, lastOdo: Int) = Reminder(
        vehicleId = vehicleId, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
        lastDoneOdometer = lastOdo, lastDoneDate = System.currentTimeMillis()
    )

    @Test
    fun `notifikasi muncul sekali saat naik ke mendekati lalu tidak diulang`() = runBlocking<Unit> {
        val v = newVehicle(10_000)
        val rid = repo.addReminder(reminder(v, lastOdo = 9_000)) // sisa 1.000 km: aman
        assertTrue(notifier.delivered.isEmpty())

        repo.addFuelLog(fuel(v, 10_850)) // sisa 150 km: mendekati
        assertEquals(listOf(rid to ReminderLevel.DUE_SOON), notifier.delivered)

        repo.addFuelLog(fuel(v, 10_900)) // masih mendekati
        assertEquals(1, notifier.delivered.size)
    }

    @Test
    fun `naik ke jatuh tempo mengirim notifikasi kedua`() = runBlocking<Unit> {
        val v = newVehicle(10_000)
        val rid = repo.addReminder(reminder(v, lastOdo = 9_000))
        repo.addFuelLog(fuel(v, 10_850))

        repo.addFuelLog(fuel(v, 11_100)) // lewat batas

        assertEquals(
            listOf(rid to ReminderLevel.DUE_SOON, rid to ReminderLevel.OVERDUE),
            notifier.delivered
        )
    }

    @Test
    fun `servis yang me-reset membatalkan notifikasi dan mengosongkan status`() = runBlocking<Unit> {
        val v = newVehicle(10_000)
        val rid = repo.addReminder(reminder(v, lastOdo = 8_000)) // sisa 0: langsung jatuh tempo
        assertEquals(listOf(rid to ReminderLevel.OVERDUE), notifier.delivered)

        repo.addServiceLog(service(v, 10_100), setOf(rid))

        assertTrue(rid in notifier.cancelled)
        val r = repo.getReminder(rid)!!
        assertEquals(0, r.lastNotifiedLevel)
        assertEquals(0, r.notifyCount)
    }

    @Test
    fun `pengingat yang ditunda tidak mengirim notifikasi`() = runBlocking<Unit> {
        val v = newVehicle(10_000)
        val rid = repo.addReminder(reminder(v, lastOdo = 9_000))
        repo.snoozeReminder(rid, System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000)

        repo.addFuelLog(fuel(v, 11_500)) // jatuh tempo, tapi sedang ditunda

        assertTrue(notifier.delivered.isEmpty())
    }

    @Test
    fun `notifikasi yang gagal tampil tidak dicatat sebagai terkirim`() = runBlocking<Unit> {
        val v = newVehicle(10_000)
        repo.addReminder(reminder(v, lastOdo = 9_000))

        notifier.canShow = false
        repo.addFuelLog(fuel(v, 10_850))
        assertTrue(notifier.delivered.isEmpty())

        notifier.canShow = true
        repo.addFuelLog(fuel(v, 10_900)) // level sama, tapi tadi belum benar-benar terkirim
        assertEquals(1, notifier.delivered.size)
    }
}