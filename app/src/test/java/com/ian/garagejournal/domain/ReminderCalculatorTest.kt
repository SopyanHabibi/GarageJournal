package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderCalculatorTest {

    private val zone = ZoneId.of("UTC")

    private fun day(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun reminder(
        km: Int? = 2_000,
        months: Int? = null,
        lastOdo: Int = 10_000,
        lastDone: Long = day(2026, 1, 1)
    ) = Reminder(
        vehicleId = 1, name = "Ganti oli", intervalKm = km, intervalMonths = months,
        lastDoneOdometer = lastOdo, lastDoneDate = lastDone
    )

    private fun status(r: Reminder, odo: Int, now: Long = day(2026, 1, 2)) =
        ReminderCalculator.status(r, odo, now, zone)

    @Test
    fun `masih jauh dari batas km berstatus OK`() {
        val s = status(reminder(), odo = 10_500)
        assertEquals(ReminderLevel.OK, s.level)
        assertEquals(1_500, s.kmRemaining)
        assertEquals(0.25f, s.progress, 0.001f)
    }

    @Test
    fun `sisa 150 km dari interval 2000 berstatus mendekati`() {
        assertEquals(ReminderLevel.DUE_SOON, status(reminder(), odo = 11_850).level)
    }

    @Test
    fun `ambang mendekati minimal 100 km untuk interval kecil`() {
        // interval 500 km: 10% = 50 km, tapi dipakai minimal 100 km
        val r = reminder(km = 500)
        assertEquals(ReminderLevel.DUE_SOON, status(r, odo = 10_420).level) // sisa 80
        assertEquals(ReminderLevel.OK, status(r, odo = 10_300).level)       // sisa 200
    }

    @Test
    fun `tepat di batas dan melewati batas berstatus jatuh tempo`() {
        assertEquals(ReminderLevel.OVERDUE, status(reminder(), odo = 12_000).level)

        val over = status(reminder(), odo = 12_300)
        assertEquals(ReminderLevel.OVERDUE, over.level)
        assertEquals(-300, over.kmRemaining)
        assertEquals(1f, over.progress, 0.001f) // progress dibatasi maksimal 1
    }

    @Test
    fun `interval waktu menghitung hari tersisa`() {
        val r = reminder(km = null, months = 2) // 1 Jan -> jatuh tempo 1 Mar
        val a = status(r, odo = 10_000, now = day(2026, 2, 10))
        assertEquals(ReminderLevel.OK, a.level)
        assertEquals(19, a.daysRemaining)

        val b = status(r, odo = 10_000, now = day(2026, 2, 20))
        assertEquals(ReminderLevel.DUE_SOON, b.level)
        assertEquals(9, b.daysRemaining)

        val c = status(r, odo = 10_000, now = day(2026, 3, 5))
        assertEquals(ReminderLevel.OVERDUE, c.level)
        assertEquals(-4, c.daysRemaining)
    }

    @Test
    fun `jika km dan waktu diisi yang paling parah menang`() {
        val r = reminder(km = 2_000, months = 1) // km masih aman, waktu sudah lewat
        val s = status(r, odo = 10_100, now = day(2026, 2, 5))
        assertEquals(ReminderLevel.OVERDUE, s.level)
    }

    @Test
    fun `tanpa interval apa pun berstatus OK`() {
        val s = status(reminder(km = null, months = null), odo = 99_999)
        assertEquals(ReminderLevel.OK, s.level)
        assertNull(s.kmRemaining)
        assertNull(s.daysRemaining)
    }
}