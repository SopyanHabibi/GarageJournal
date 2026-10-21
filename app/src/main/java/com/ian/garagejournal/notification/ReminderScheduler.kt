package com.ian.garagejournal.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val DAILY_NAME = "reminder-check-daily"
    private const val ON_OPEN_NAME = "reminder-check-on-open"

    /** Pengecekan harian di latar belakang, kira-kira jam 09.00. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextNineAm())
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Satu kali cek setiap app dibuka: cadangan kalau HP menunda pekerjaan latar belakang. */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ON_OPEN_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderCheckWorker>().build()
        )
    }

    private fun delayUntilNextNineAm(): Duration {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(9, 0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}