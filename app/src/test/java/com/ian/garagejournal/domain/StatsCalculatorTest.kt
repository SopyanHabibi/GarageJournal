package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.ServiceLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatsCalculatorTest {

    private val zone = ZoneId.of("UTC")

    private fun day(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun service(date: Long, cost: Long) =
        ServiceLog(vehicleId = 1, date = date, odometer = 0, title = "Servis", cost = cost)

    private fun fuel(date: Long, total: Long, km: Double? = null, odo: Int = 0) = FuelLog(
        vehicleId = 1, date = date, odometer = odo, liters = 1.0, pricePerLiter = total,
        totalCost = total, isFullTank = true, kmPerLiter = km
    )

    @Test
    fun `biaya per bulan mencakup bulan kosong dan urut dari terlama`() {
        val costs = StatsCalculator.monthlyCosts(
            serviceLogs = listOf(service(day(2026, 9, 10), 50_000)),
            fuelLogs = listOf(fuel(day(2026, 10, 3), 30_000)),
            months = 3, nowMillis = day(2026, 10, 15), zone = zone
        )
        assertEquals(
            listOf(
                MonthlyCost(YearMonth.of(2026, 8), 0, 0),
                MonthlyCost(YearMonth.of(2026, 9), 50_000, 0),
                MonthlyCost(YearMonth.of(2026, 10), 0, 30_000)
            ),
            costs
        )
    }

    @Test
    fun `rentang bulan melewati pergantian tahun`() {
        val costs = StatsCalculator.monthlyCosts(
            emptyList(), emptyList(), months = 4, nowMillis = day(2027, 2, 10), zone = zone
        )
        assertEquals(
            listOf(
                YearMonth.of(2026, 11), YearMonth.of(2026, 12),
                YearMonth.of(2027, 1), YearMonth.of(2027, 2)
            ),
            costs.map { it.month }
        )
    }

    @Test
    fun `catatan di luar rentang diabaikan`() {
        val costs = StatsCalculator.monthlyCosts(
            listOf(service(day(2026, 1, 5), 999_000)), emptyList(),
            months = 3, nowMillis = day(2026, 10, 15), zone = zone
        )
        assertEquals(0L, costs.sumOf { it.total })
    }

    @Test
    fun `beberapa catatan di bulan yang sama dijumlahkan`() {
        val costs = StatsCalculator.monthlyCosts(
            serviceLogs = listOf(service(day(2026, 10, 2), 50_000), service(day(2026, 10, 20), 25_000)),
            fuelLogs = listOf(fuel(day(2026, 10, 5), 30_000), fuel(day(2026, 10, 25), 20_000)),
            months = 1, nowMillis = day(2026, 10, 28), zone = zone
        )
        assertEquals(listOf(MonthlyCost(YearMonth.of(2026, 10), 75_000, 50_000)), costs)
    }

    @Test
    fun `bulan dihitung menurut zona waktu lokal`() {
        val jakarta = ZoneId.of("Asia/Jakarta")
        // 30 Sep 20:00 UTC = 1 Okt 03:00 WIB, jadi masuk Oktober
        val lateNight = Instant.parse("2026-09-30T20:00:00Z").toEpochMilli()
        val costs = StatsCalculator.monthlyCosts(
            listOf(service(lateNight, 40_000)), emptyList(),
            months = 2, nowMillis = day(2026, 10, 15), zone = jakarta
        )
        assertEquals(0L, costs[0].serviceCost) // September
        assertEquals(40_000L, costs[1].serviceCost) // Oktober
    }

    @Test
    fun `titik efisiensi hanya yang punya hasil dan diurutkan menurut tanggal`() {
        val points = StatsCalculator.efficiencyPoints(
            listOf(
                fuel(day(2026, 10, 10), 0, km = 48.0, odo = 10_300),
                fuel(day(2026, 10, 1), 0, km = null, odo = 10_000),
                fuel(day(2026, 10, 5), 0, km = 50.0, odo = 10_150)
            )
        )
        assertEquals(listOf(50.0, 48.0), points.map { it.kmPerLiter })
    }

    @Test
    fun `rata-rata bulanan mengabaikan bulan kosong di awal`() {
        val list = listOf(0L, 0L, 100_000L, 200_000L)
            .mapIndexed { i, total -> MonthlyCost(YearMonth.of(2026, 7 + i), total, 0) }

        assertEquals(150_000L, StatsCalculator.averageMonthlyCost(list))
        assertEquals(0L, StatsCalculator.averageMonthlyCost(list.map { it.copy(serviceCost = 0) }))
    }
}