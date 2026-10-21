package com.ian.garagejournal.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ian.garagejournal.GarageApp
import com.ian.garagejournal.domain.ReminderNotificationPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val action = intent.action
        if (reminderId == -1L || action == null) return

        val app = context.applicationContext as GarageApp
        val pending = goAsync() // beri waktu untuk kerja database sebelum receiver dimatikan
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DONE -> app.repository.markReminderDoneNow(reminderId)
                    ACTION_SNOOZE -> app.repository.snoozeReminder(
                        reminderId,
                        System.currentTimeMillis() + ReminderNotificationPolicy.SNOOZE_MILLIS
                    )
                }
                app.reminderNotifier.cancel(reminderId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.ian.garagejournal.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.ian.garagejournal.action.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}