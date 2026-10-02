package ru.kinopolka.core.data.cleanup;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import java.util.concurrent.TimeUnit;

/** Runs {@link CacheCleaner} once a day while the battery is not low. */
@HiltWorker
public final class CacheCleanupWorker extends Worker {

    private static final String WORK_NAME = "cache-cleanup";

    private final CacheCleaner cleaner;

    @AssistedInject
    public CacheCleanupWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters params,
            CacheCleaner cleaner) {
        super(context, params);
        this.cleaner = cleaner;
    }

    @NonNull
    @Override
    public Result doWork() {
        cleaner.clean();
        return Result.success();
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CacheCleanupWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
