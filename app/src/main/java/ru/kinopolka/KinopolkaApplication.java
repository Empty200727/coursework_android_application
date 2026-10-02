package ru.kinopolka;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import com.google.android.material.color.DynamicColors;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;
import ru.kinopolka.core.data.cleanup.CacheCleanupWorker;

@HiltAndroidApp
public class KinopolkaApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        // Material 3 dynamic color on Android 12+, the brand palette otherwise.
        DynamicColors.applyToActivitiesIfAvailable(this);
        CacheCleanupWorker.schedule(this);
    }

    /** WorkManager creates workers through Hilt (its default initializer is removed in the manifest). */
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setWorkerFactory(workerFactory).build();
    }
}
