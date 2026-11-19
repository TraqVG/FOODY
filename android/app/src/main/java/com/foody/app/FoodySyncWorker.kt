package com.foody.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// WorkManager is a 15-minute best-effort backup. Foreground polling handles
// timely notifications while the user is actively using Foody.
class FoodySyncWorker(
    context: Context,
    params: WorkerParameters
): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val repo=FoodyRepository(applicationContext as android.app.Application)
        if(!repo.tokenPresent() || !repo.notificationsEnabled()) return Result.success()
        return try {
            val response=repo.events()
            response.events.forEach { FoodyNotifier.show(applicationContext,it) }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
