package com.x.player.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.x.player.domain.isAlbumUri
import java.util.concurrent.Executors

/** Lets the media notification / lock screen show album art on Android 10+. */
@OptIn(UnstableApi::class)
class AlbumBitmapLoader(private val context: Context) : BitmapLoader {

    private val delegate = DataSourceBitmapLoader(context)
    private val executor = Executors.newSingleThreadExecutor()

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAlbumUri(uri)) {
            val future = SettableFuture.create<Bitmap>()
            executor.execute {
                try {
                    future.set(context.contentResolver.loadThumbnail(uri, Size(512, 512), null))
                } catch (e: Exception) {
                    future.setException(e)
                }
            }
            return future
        }
        return delegate.loadBitmap(uri)
    }
}
