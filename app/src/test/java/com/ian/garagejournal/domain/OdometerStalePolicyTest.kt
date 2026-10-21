package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerStalePolicyTest {

    private val day = 24L * 60 * 60 * 1000

    private fun vehicle(updatedAt: Long = 0L, notifiedAt: Long = 0L, count: Int = 0) = Vehicle(
        id = 1, name = "Si Hitam", brand = "Honda", model = "Beat", year = 2020,
        plateNumber = "BK 1 X", currentOdometer = 10_000, createdAt = 0L,
        odometerUpdatedAt = updatedAt, staleNotifiedAt = notifiedAt, staleNotifyCount = count
    )

    @Test
    fun `belum 30 hari belum dianggap basi`() {
        assertFalse(OdometerStalePolicy.shouldNotify(vehicle(), 29 * day))
    }

    @Test
    fun `tepat 30 hari notifikasi pertama dikirim`() {
        assertTrue(OdometerStalePolicy.shouldNotify(vehicle(), 30 * day))
        assertEquals(30, OdometerStalePolicy.daysSinceUpdate(vehicle(), 30 * day))
    }

    @Test
    fun `pengulangan tiap 14 hari`() {
        val v = vehicle(notifiedAt = 30 * day, count = 1)
        assertFalse(OdometerStalePolicy.shouldNotify(v, 43 * day))
        assertTrue(OdometerStalePolicy.shouldNotify(v, 44 * day))
    }

    @Test
    fun `berhenti setelah 3 kali`() {
        assertFalse(OdometerStalePolicy.shouldNotify(vehicle(notifiedAt = 60 * day, count = 3), 365 * day))
    }
}