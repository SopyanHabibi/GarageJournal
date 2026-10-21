package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder

data class NotifyDecision(val shouldNotify: Boolean, val updated: Reminder)

object ReminderNotificationPolicy {
    const val SNOOZE_MILLIS = 3L * 24 * 60 * 60 * 1000 // 3 hari
    private const val DAY = 24L * 60 * 60 * 1000

    /** Jeda (hari) sebelum pengulangan ke-1, ke-2, dst. Panjang list = jumlah maksimal pengulangan. */
    fun repeatGapsDays(level: ReminderLevel): List<Int> = when (level) {
        ReminderLevel.OK -> emptyList()
        ReminderLevel.DUE_SOON -> listOf(3, 3, 3)
        ReminderLevel.OVERDUE -> listOf(2, 3, 5, 7, 7, 7) // kumulatif: hari ke-2, 5, 10, 17, 24, 31
    }

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

    /** Dipanggil oleh pengecekan berkala: aturan perubahan data + pengulangan berjeda dengan batas. */
    fun onPeriodicCheck(reminder: Reminder, level: ReminderLevel, nowMillis: Long): NotifyDecision {
        val base = onDataChanged(reminder, level, nowMillis)
        if (base.shouldNotify) return base // level naik

        // Pengulangan hanya untuk level yang sama dengan yang terakhir dinotifikasi
        if (level == ReminderLevel.OK || level.code != reminder.lastNotifiedLevel) return base
        if (reminder.snoozedUntil > nowMillis) return base

        val gaps = repeatGapsDays(level)
        val sent = reminder.notifyCount
        if (sent < 1 || sent > gaps.size) return base // belum pernah kirim, atau batas pengulangan tercapai

        if (nowMillis - reminder.lastNotifiedAt < gaps[sent - 1] * DAY) return base

        return NotifyDecision(true, reminder.copy(lastNotifiedAt = nowMillis, notifyCount = sent + 1))
    }
}