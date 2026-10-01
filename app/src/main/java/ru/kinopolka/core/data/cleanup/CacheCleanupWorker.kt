package ru.kinopolka.core.data.cleanup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Runs [CacheCleaner] once a day while the battery is not low. */
@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleaner: CacheCleaner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        cleaner.clean()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "cache-cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
