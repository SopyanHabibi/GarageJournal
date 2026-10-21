package com.ian.garagejournal.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ian.garagejournal.MainActivity
import com.ian.garagejournal.R
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderNotifier
import com.ian.garagejournal.domain.ReminderStatus
import com.ian.garagejournal.domain.summaryText
import java.time.LocalTime

class AndroidReminderNotifier(private val context: Context) : ReminderNotifier {

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SOON, "Servis mendekati", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pengingat saat jadwal servis sudah dekat"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_OVERDUE, "Servis jatuh tempo", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat saat jadwal servis sudah terlewati"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STALE, "Pengingat odometer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ajakan memperbarui odometer supaya pengingat servis tetap akurat"
            }
        )
    }

    @SuppressLint("MissingPermission") // izin dicek lewat areNotificationsEnabled()
    override fun show(reminder: Reminder, vehicle: Vehicle, status: ReminderStatus): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        val overdue = status.level == ReminderLevel.OVERDUE
        val notification = NotificationCompat.Builder(context, if (overdue) CHANNEL_OVERDUE else CHANNEL_SOON)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (overdue) "${reminder.name} sudah jatuh tempo" else "${reminder.name} sudah dekat")
            .setContentText("${vehicle.name} • ${status.summaryText()}")
            .setPriority(if (overdue) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(isQuietHours()) // malam hari: tanpa suara/getar
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(reminder.id))
            .addAction(0, "Sudah diservis", actionIntent(ReminderActionReceiver.ACTION_DONE, reminder.id, 1))
            .addAction(0, "Tunda 3 hari", actionIntent(ReminderActionReceiver.ACTION_SNOOZE, reminder.id, 2))
            .build()

        manager.notify(reminder.id.toInt(), notification)
        return true
    }

    override fun cancel(reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    @SuppressLint("MissingPermission")
    override fun showStaleOdometer(vehicle: Vehicle, daysSinceUpdate: Int): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        val notification = NotificationCompat.Builder(context, CHANNEL_STALE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Odometer ${vehicle.name} belum diperbarui")
            .setContentText("Sudah $daysSinceUpdate hari. Perbarui supaya pengingat servis tetap akurat.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(STALE_ID_BASE.toLong() + vehicle.id))
            .build()

        manager.notify(staleNotificationId(vehicle.id), notification)
        return true
    }

    override fun cancelStale(vehicleId: Long) {
        NotificationManagerCompat.from(context).cancel(staleNotificationId(vehicleId))
    }

    private fun isQuietHours(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 22 || hour < 6
    }

    private fun staleNotificationId(vehicleId: Long): Int = STALE_ID_BASE + vehicleId.toInt()

    private fun openAppIntent(key: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode(key, 0), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun actionIntent(action: String, reminderId: Long, slot: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context, requestCode(reminderId, slot), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // Kode unik per pengingat dan per tombol. Tanpa ini, tombol milik pengingat lain bisa tertukar.
    private fun requestCode(key: Long, slot: Int): Int = (key * 4 + slot).toInt()

    companion object {
        const val CHANNEL_SOON = "reminder_soon"
        const val CHANNEL_OVERDUE = "reminder_overdue"
        const val CHANNEL_STALE = "odometer_stale"
        private const val STALE_ID_BASE = 1_000_000 // supaya tidak bertabrakan dengan id pengingat
    }
}