package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationPolicyTest {

    private val now = 1_000_000L

    private fun reminder(stored: Int = 0, snoozedUntil: Long = 0L, count: Int = 0) = Reminder(
        id = 1, vehicleId = 1, name = "Ganti oli", intervalKm = 2_000, intervalMonths = null,
        lastDoneOdometer = 0, lastDoneDate = 0L,
        lastNotifiedLevel = stored, notifyCount = count, snoozedUntil = snoozedUntil
    )

    @Test
    fun `naik dari aman ke mendekati mengirim notifikasi dan mencatatnya`() {
        val d = ReminderNotificationPolicy.onDataChanged(reminder(), ReminderLevel.DUE_SOON, now)
        assertTrue(d.shouldNotify)
        assertEquals(1, d.updated.lastNotifiedLevel)
        assertEquals(now, d.updated.lastNotifiedAt)
        assertEquals(1, d.updated.notifyCount)
    }

    @Test
    fun `level yang sama tidak mengirim ulang`() {
        val r = reminder(stored = 1, count = 1)
        val d = ReminderNotificationPolicy.onDataChanged(r, ReminderLevel.DUE_SOON, now)
        assertFalse(d.shouldNotify)
        assertEquals(r, d.updated)
    }

    @Test
    fun `naik dari mendekati ke jatuh tempo mengirim notifikasi lagi`() {
        val d = ReminderNotificationPolicy.onDataChanged(reminder(stored = 1), ReminderLevel.OVERDUE, now)
        assertTrue(d.shouldNotify)
        assertEquals(2, d.updated.lastNotifiedLevel)
    }

    @Test
    fun `saat ditunda tidak mengirim dan status tidak berubah`() {
        val r = reminder(snoozedUntil = now + 1_000)
        val d = ReminderNotificationPolicy.onDataChanged(r, ReminderLevel.OVERDUE, now)
        assertFalse(d.shouldNotify)
        assertEquals(r, d.updated)
    }

    @Test
    fun `tunda yang sudah lewat tidak lagi menahan notifikasi`() {
        val r = reminder(snoozedUntil = now - 1)
        assertTrue(ReminderNotificationPolicy.onDataChanged(r, ReminderLevel.OVERDUE, now).shouldNotify)
    }

    @Test
    fun `kembali aman membersihkan seluruh status notifikasi`() {
        val r = reminder(stored = 2, snoozedUntil = now + 1_000, count = 3)
        val d = ReminderNotificationPolicy.onDataChanged(r, ReminderLevel.OK, now)
        assertFalse(d.shouldNotify)
        assertEquals(0, d.updated.lastNotifiedLevel)
        assertEquals(0, d.updated.notifyCount)
        assertEquals(0L, d.updated.snoozedUntil)
    }

    @Test
    fun `level turun tapi belum aman hanya menurunkan catatan`() {
        val d = ReminderNotificationPolicy.onDataChanged(reminder(stored = 2), ReminderLevel.DUE_SOON, now)
        assertFalse(d.shouldNotify)
        assertEquals(1, d.updated.lastNotifiedLevel)
    }
}