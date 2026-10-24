package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.ServiceLog
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class EfficiencyPoint(val date: Long, val odometer: Int, val kmPerLiter: Double)

data class MonthlyCost(val month: YearMonth, val serviceCost: Long, val fuelCost: Long) {
    val total: Long get() = serviceCost + fuelCost
}

object StatsCalculator {

    /** Titik tren KM/L: hanya log yang sudah punya hasil hitung, urut menurut tanggal lalu odometer. */
    fun efficiencyPoints(fuelLogs: List<FuelLog>): List<EfficiencyPoint> =
        fuelLogs
            .mapNotNull { log -> log.kmPerLiter?.let { EfficiencyPoint(log.date, log.odometer, it) } }
            .sortedWith(compareBy({ it.date }, { it.odometer }))

    /** Biaya servis dan BBM untuk N bulan terakhir (termasuk bulan ini), dari yang terlama. Bulan kosong tetap ada. */
    fun monthlyCosts(
        serviceLogs: List<ServiceLog>,
        fuelLogs: List<FuelLog>,
        months: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<MonthlyCost> {
        val current = monthOf(nowMillis, zone)
        val serviceByMonth = serviceLogs
            .groupBy { monthOf(it.date, zone) }
            .mapValues { (_, logs) -> logs.sumOf { it.cost } }
        val fuelByMonth = fuelLogs
            .groupBy { monthOf(it.date, zone) }
            .mapValues { (_, logs) -> logs.sumOf { it.totalCost } }

        return (months - 1 downTo 0).map { back ->
            val m = current.minusMonths(back.toLong())
            MonthlyCost(m, serviceByMonth[m] ?: 0L, fuelByMonth[m] ?: 0L)
        }
    }

    /** Rata-rata biaya per bulan, dihitung sejak bulan pertama yang ada datanya (bulan kosong di awal diabaikan). */
    fun averageMonthlyCost(costs: List<MonthlyCost>): Long {
        val active = costs.dropWhile { it.total == 0L }
        return if (active.isEmpty()) 0L else active.sumOf { it.total } / active.size
    }

    private fun monthOf(millis: Long, zone: ZoneId): YearMonth =
        YearMonth.from(Instant.ofEpochMilli(millis).atZone(zone))
}