package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder

data class NotifyDecision(val shouldNotify: Boolean, val updated: Reminder)

object ReminderNotificationPolicy {
    const val SNOOZE_MILLIS = 3L * 24 * 60 * 60 * 1000 // 3 hari

    /** Dipanggil setiap data berubah. Notifikasi hanya muncul kalau level NAIK dan tidak sedang ditunda. */
    fun onDataChanged(reminder: Reminder, level: ReminderLevel, nowMillis: Long): NotifyDecision {
        val current = level.code
        val stored = reminder.lastNotifiedLevel
        return when {
            // Kembali aman (mis. setelah servis): bersihkan semua status notifikasi
            current == ReminderLevel.OK.code -> NotifyDecision(
                false,
                reminder.copy(lastNotifiedLevel = 0, lastNotifiedAt = 0L, notifyCount = 0, snoozedUntil = 0L)
            )
            // Level turun tapi belum aman (mis. interval diubah): catat supaya kenaikan berikutnya tetap dinotifikasi
            current < stored -> NotifyDecision(false, reminder.copy(lastNotifiedLevel = current))
            // Level sama: sudah pernah dinotifikasi
            current == stored -> NotifyDecision(false, reminder)
            // Sedang ditunda
            reminder.snoozedUntil > nowMillis -> NotifyDecision(false, reminder)
            else -> NotifyDecision(
                true,
                reminder.copy(lastNotifiedLevel = current, lastNotifiedAt = nowMillis, notifyCount = 1)
            )
        }
    }
}