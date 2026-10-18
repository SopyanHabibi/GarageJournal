package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.FuelLog

object FuelCalculator {

    private val order = compareBy<FuelLog>({ it.odometer }, { it.date }, { it.id })

    /**
     * KM/L untuk tiap pengisian penuh yang punya pembanding. Kunci map = id log.
     * Log yang tidak ada di map berarti belum bisa dihitung (pengisian awal / sebagian).
     */
    fun efficiencies(logs: List<FuelLog>): Map<Long, Double> {
        val result = mutableMapOf<Long, Double>()
        var lastFullOdo: Int? = null
        var litersSinceFull = 0.0

        for (log in logs.sortedWith(order)) {
            if (log.isFullTank) {
                val base = lastFullOdo
                if (base != null) {
                    val distance = log.odometer - base
                    val liters = litersSinceFull + log.liters
                    if (distance > 0 && liters > 0) result[log.id] = distance / liters
                }
                lastFullOdo = log.odometer
                litersSinceFull = 0.0
            } else {
                litersSinceFull += log.liters
            }
        }
        return result
    }

    /** Efisiensi keseluruhan: total jarak / total liter antara pengisian penuh pertama dan terakhir. */
    fun overall(logs: List<FuelLog>): Double? {
        val sorted = logs.sortedWith(order)
        val first = sorted.indexOfFirst { it.isFullTank }
        val last = sorted.indexOfLast { it.isFullTank }
        if (first == -1 || first == last) return null

        val distance = sorted[last].odometer - sorted[first].odometer
        val liters = sorted.subList(first + 1, last + 1).sumOf { it.liters }
        return if (distance > 0 && liters > 0) distance / liters else null
    }
}