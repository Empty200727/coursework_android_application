package ru.kinopolka.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    /** N-03: the disk cache of images is limited to 250 MB. */
    private const val IMAGE_DISK_CACHE_BYTES = 250L * 1024 * 1024

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, client: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
}
