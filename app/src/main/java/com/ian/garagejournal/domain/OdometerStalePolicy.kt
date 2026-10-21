package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Vehicle

object OdometerStalePolicy {
    const val STALE_AFTER_DAYS = 30
    const val REPEAT_EVERY_DAYS = 14
    const val MAX_NOTIFICATIONS = 3
    private const val DAY = 24L * 60 * 60 * 1000

    fun daysSinceUpdate(vehicle: Vehicle, nowMillis: Long): Int =
        ((nowMillis - vehicle.odometerUpdatedAt) / DAY).toInt()

    fun shouldNotify(vehicle: Vehicle, nowMillis: Long): Boolean {
        if (daysSinceUpdate(vehicle, nowMillis) < STALE_AFTER_DAYS) return false
        return when {
            vehicle.staleNotifyCount >= MAX_NOTIFICATIONS -> false
            vehicle.staleNotifyCount == 0 -> true
            else -> nowMillis - vehicle.staleNotifiedAt >= REPEAT_EVERY_DAYS * DAY
        }
    }
}