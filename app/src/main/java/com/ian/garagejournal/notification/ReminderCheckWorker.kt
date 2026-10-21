package com.ian.garagejournal.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ian.garagejournal.GarageApp
import kotlin.coroutines.cancellation.CancellationException

class ReminderCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GarageApp
        return try {
            app.repository.runPeriodicCheck()
            Result.success()
        } catch (e: CancellationException) {
            throw e // pembatalan harus diteruskan, jangan ditelan
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}