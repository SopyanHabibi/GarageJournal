package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ReminderLevel(val code: Int) { OK(0), DUE_SOON(1), OVERDUE(2) } // urutan = tingkat keparahan

data class ReminderStatus(
    val level: ReminderLevel,
    val kmRemaining: Int?,    // negatif = sudah lewat
    val daysRemaining: Int?,  // negatif = sudah lewat
    val progress: Float       // 0..1 untuk progress bar
)

data class ReminderWithStatus(val reminder: Reminder, val status: ReminderStatus)

object ReminderCalculator {
    const val SOON_DAYS = 14
    private const val SOON_KM_PERCENT = 10
    private const val SOON_KM_MIN = 100

    fun status(
        reminder: Reminder,
        currentOdometer: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): ReminderStatus {
        var level = ReminderLevel.OK
        var progress = 0f
        var kmRemaining: Int? = null
        var daysRemaining: Int? = null

        reminder.intervalKm?.takeIf { it > 0 }?.let { interval ->
            val remaining = reminder.lastDoneOdometer + interval - currentOdometer
            val soonThreshold = maxOf(SOON_KM_MIN, interval * SOON_KM_PERCENT / 100)
            kmRemaining = remaining
            level = maxOf(level, levelFor(remaining, soonThreshold))
            val used = (currentOdometer - reminder.lastDoneOdometer).coerceAtLeast(0)
            progress = maxOf(progress, used.toFloat() / interval)
        }

        reminder.intervalMonths?.takeIf { it > 0 }?.let { months ->
            val lastDone = Instant.ofEpochMilli(reminder.lastDoneDate).atZone(zone).toLocalDate()
            val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            val due = lastDone.plusMonths(months.toLong())
            val remaining = ChronoUnit.DAYS.between(today, due).toInt()
            val total = ChronoUnit.DAYS.between(lastDone, due).toInt()
            daysRemaining = remaining
            level = maxOf(level, levelFor(remaining, SOON_DAYS))
            val elapsed = ChronoUnit.DAYS.between(lastDone, today).toInt().coerceAtLeast(0)
            if (total > 0) progress = maxOf(progress, elapsed.toFloat() / total)
        }

        return ReminderStatus(level, kmRemaining, daysRemaining, progress.coerceIn(0f, 1f))
    }

    private fun levelFor(remaining: Int, soonThreshold: Int): ReminderLevel = when {
        remaining <= 0 -> ReminderLevel.OVERDUE
        remaining <= soonThreshold -> ReminderLevel.DUE_SOON
        else -> ReminderLevel.OK
    }
}