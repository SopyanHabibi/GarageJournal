package com.ian.garagejournal.domain

import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.Vehicle

interface ReminderNotifier {
    /** @return true kalau notifikasi benar-benar tampil (false mis. karena izin ditolak) */
    fun show(reminder: Reminder, vehicle: Vehicle, status: ReminderStatus): Boolean
    fun cancel(reminderId: Long)

    companion object {
        val None: ReminderNotifier = object : ReminderNotifier {
            override fun show(reminder: Reminder, vehicle: Vehicle, status: ReminderStatus) = false
            override fun cancel(reminderId: Long) {}
        }
    }
}