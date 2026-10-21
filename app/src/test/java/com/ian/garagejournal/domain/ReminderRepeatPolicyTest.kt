package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRepeatPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 10 * day // waktu notifikasi terakhir

    private fun reminder(level: Int, count: Int, snoozedUntil: Long = 0L) = Reminder(
        id = 1, vehicleId = 1, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
        lastDoneOdometer = 0, lastDoneDate = 0L,
        lastNotifiedLevel = level, lastNotifiedAt = t0, notifyCount = count, snoozedUntil = snoozedUntil
    )

    private fun check(r: Reminder, level: ReminderLevel, now: Long) =
        ReminderNotificationPolicy.onPeriodicCheck(r, level, now)

    @Test
    fun `jatuh tempo diulang setelah 2 hari lalu 3 hari`() {
        val first = reminder(level = 2, count = 1)
        assertFalse(check(first, ReminderLevel.OVERDUE, t0 + day).shouldNotify)

        val repeat1 = check(first, ReminderLevel.OVERDUE, t0 + 2 * day)
        assertTrue(repeat1.shouldNotify)
        assertEquals(2, repeat1.updated.notifyCount)
        assertEquals(t0 + 2 * day, repeat1.updated.lastNotifiedAt)

        assertFalse(check(repeat1.updated, ReminderLevel.OVERDUE, t0 + 4 * day).shouldNotify)
        assertTrue(check(repeat1.updated, ReminderLevel.OVERDUE, t0 + 5 * day).shouldNotify)
    }

    @Test
    fun `jatuh tempo berhenti setelah 6 pengulangan`() {
        // count 6 masih boleh mengulang sekali lagi (jeda 7 hari), count 7 sudah batas
        assertTrue(check(reminder(2, count = 6), ReminderLevel.OVERDUE, t0 + 7 * day).shouldNotify)
        assertFalse(check(reminder(2, count = 7), ReminderLevel.OVERDUE, t0 + 365 * day).shouldNotify)
    }

    @Test
    fun `mendekati diulang tiap 3 hari maksimal 3 kali`() {
        assertTrue(check(reminder(1, count = 1), ReminderLevel.DUE_SOON, t0 + 3 * day).shouldNotify)
        assertTrue(check(reminder(1, count = 3), ReminderLevel.DUE_SOON, t0 + 3 * day).shouldNotify)
        assertFalse(check(reminder(1, count = 4), ReminderLevel.DUE_SOON, t0 + 365 * day).shouldNotify)
    }

    @Test
    fun `tunda menahan pengulangan lalu berlanjut setelah lewat`() {
        val r = reminder(2, count = 1, snoozedUntil = t0 + 5 * day)
        assertFalse(check(r, ReminderLevel.OVERDUE, t0 + 3 * day).shouldNotify)
        assertTrue(check(r, ReminderLevel.OVERDUE, t0 + 5 * day + 1).shouldNotify)
    }

    @Test
    fun `kenaikan level tetap diprioritaskan`() {
        val d = check(reminder(1, count = 2), ReminderLevel.OVERDUE, t0 + 60 * 60 * 1000)
        assertTrue(d.shouldNotify)
        assertEquals(2, d.updated.lastNotifiedLevel)
        assertEquals(1, d.updated.notifyCount)
    }

    @Test
    fun `aman tidak mengulang dan membersihkan status`() {
        val d = check(reminder(2, count = 3), ReminderLevel.OK, t0 + 30 * day)
        assertFalse(d.shouldNotify)
        assertEquals(0, d.updated.notifyCount)
    }

    @Test
    fun `tanpa hitungan notifikasi tidak ada pengulangan`() {
        assertFalse(check(reminder(2, count = 0), ReminderLevel.OVERDUE, t0 + 30 * day).shouldNotify)
    }
}