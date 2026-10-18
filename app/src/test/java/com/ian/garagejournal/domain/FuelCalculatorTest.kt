package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.FuelLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelCalculatorTest {

    private fun log(id: Long, odo: Int, liters: Double, full: Boolean = true) = FuelLog(
        id = id, vehicleId = 1, date = odo.toLong(), odometer = odo,
        liters = liters, pricePerLiter = 10_000, totalCost = (liters * 10_000).toLong(),
        isFullTank = full
    )

    @Test
    fun `dua pengisian penuh menghasilkan km per liter`() {
        val result = FuelCalculator.efficiencies(listOf(log(1, 10_000, 4.0), log(2, 10_150, 3.0)))
        assertNull(result[1L])                  // pengisian awal, belum ada pembanding
        assertEquals(50.0, result[2L]!!, 0.001) // 150 km / 3 L
    }

    @Test
    fun `pengisian sebagian ikut dijumlahkan ke liter terpakai`() {
        val result = FuelCalculator.efficiencies(
            listOf(
                log(1, 10_000, 4.0),
                log(2, 10_050, 1.0, full = false),
                log(3, 10_150, 2.0)
            )
        )
        assertNull(result[2L])
        assertEquals(50.0, result[3L]!!, 0.001) // 150 km / (1 + 2) L
    }

    @Test
    fun `urutan input tidak memengaruhi hasil`() {
        val result = FuelCalculator.efficiencies(listOf(log(2, 10_150, 3.0), log(1, 10_000, 4.0)))
        assertEquals(50.0, result[2L]!!, 0.001)
    }

    @Test
    fun `tanpa dua pengisian penuh tidak ada hasil`() {
        assertTrue(FuelCalculator.efficiencies(listOf(log(1, 10_000, 4.0))).isEmpty())
        assertTrue(
            FuelCalculator.efficiencies(
                listOf(log(1, 10_000, 2.0, full = false), log(2, 10_100, 2.0, full = false))
            ).isEmpty()
        )
    }

    @Test
    fun `efisiensi keseluruhan dihitung berbobot`() {
        val logs = listOf(log(1, 10_000, 4.0), log(2, 10_150, 3.0), log(3, 10_300, 6.0))
        // per interval: 50 dan 25. Keseluruhan: 300 km / 9 L, bukan rata-rata biasa 37,5
        assertEquals(300.0 / 9.0, FuelCalculator.overall(logs)!!, 0.001)
    }
}