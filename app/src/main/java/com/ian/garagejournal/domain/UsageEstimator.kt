package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.OdometerPoint

object UsageEstimator {
    const val RECENT_DAYS = 90
    const val MIN_SPAN_DAYS = 7
    private const val DAY = 24L * 60 * 60 * 1000

    fun averageKmPerDay(points: List<OdometerPoint>, nowMillis: Long): Double? {
        val recent = points.filter { nowMillis - it.date <= RECENT_DAYS * DAY }
        return estimate(recent) ?: estimate(points)
    }

    private fun estimate(points: List<OdometerPoint>): Double? {
        if (points.size < 2) return null
        val first = points.minBy { it.date }
        val last = points.maxBy { it.date }
        val spanDays = (last.date - first.date) / DAY.toDouble()
        if (spanDays < MIN_SPAN_DAYS) return null
        val km = last.odometer - first.odometer
        return if (km > 0) km / spanDays else null
    }
}