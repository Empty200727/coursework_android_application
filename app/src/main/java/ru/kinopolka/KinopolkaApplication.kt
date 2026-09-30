package ru.kinopolka

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KinopolkaApplication :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    /** Coil uses the image loader configured in ImageLoaderModule (250 MB disk cache). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
