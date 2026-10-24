package com.ranzz.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.ranzz.music.data.auth.AuthManager
import com.ranzz.music.data.remote.RanzzDownloader
import org.schabi.newpipe.extractor.NewPipe

class RanzzMusicApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
        com.ranzz.music.data.settings.SettingsManager.init(this)
        val downloader = RanzzDownloader.init(this)
        NewPipe.init(downloader)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}
