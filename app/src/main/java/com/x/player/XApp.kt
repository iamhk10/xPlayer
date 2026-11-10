package com.x.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.x.player.data.AlbumArtFetcher
import com.x.player.di.AppContainer

class XApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(AlbumArtFetcher.Factory(this@XApp)) }
            .crossfade(true)
            .build()
}
