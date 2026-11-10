package com.x.player.data

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.x.player.domain.isAlbumUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coil fetcher for `content://media/external/audio/albums/<id>` using ContentResolver.loadThumbnail. */
class AlbumArtFetcher(
    private val context: Context,
    private val uri: Uri,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK,
            )
        } catch (e: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return if (isAlbumUri(data)) AlbumArtFetcher(context, data) else null
        }
    }
}
