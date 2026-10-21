package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.OdometerPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageEstimatorTest {

    private val day = 24L * 60 * 60 * 1000

    private fun pt(dayNumber: Int, odo: Int) = OdometerPoint(dayNumber * day, odo)

    @Test
    fun `kurang dari dua titik tidak bisa diperkirakan`() {
        assertNull(UsageEstimator.averageKmPerDay(listOf(pt(0, 1_000)), 10 * day))
    }

    @Test
    fun `rentang kurang dari 7 hari tidak bisa diperkirakan`() {
        assertNull(UsageEstimator.averageKmPerDay(listOf(pt(0, 1_000), pt(5, 1_500)), 5 * day))
    }

    @Test
    fun `menghitung rata-rata km per hari`() {
        val avg = UsageEstimator.averageKmPerDay(listOf(pt(0, 10_000), pt(10, 11_000)), 10 * day)
        assertEquals(100.0, avg!!, 0.001)
    }

    @Test
    fun `memakai 90 hari terakhir kalau datanya cukup`() {
        val points = listOf(pt(0, 0), pt(20, 2_000), pt(150, 5_000), pt(160, 5_200))
        // data lama 100 km/hari diabaikan; 90 hari terakhir: 200 km dalam 10 hari
        assertEquals(20.0, UsageEstimator.averageKmPerDay(points, 160 * day)!!, 0.001)
    }

    @Test
    fun `kalau data terbaru tidak cukup pakai semua data`() {
        val points = listOf(pt(0, 0), pt(100, 3_000))
        assertEquals(30.0, UsageEstimator.averageKmPerDay(points, 200 * day)!!, 0.001)
    }

    @Test
    fun `odometer tidak bertambah tidak ada perkiraan`() {
        assertNull(UsageEstimator.averageKmPerDay(listOf(pt(0, 5_000), pt(20, 5_000)), 20 * day))
    }
}