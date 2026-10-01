package ru.kinopolka

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import ru.kinopolka.core.data.cleanup.CacheCleanupWorker

@HiltAndroidApp
class KinopolkaApplication :
    Application(),
    SingletonImageLoader.Factory,
    Configuration.Provider {

    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** WorkManager creates workers through Hilt (its default initializer is removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        CacheCleanupWorker.schedule(this)
    }

    /** Coil uses the image loader configured in ImageLoaderModule (250 MB disk cache). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
