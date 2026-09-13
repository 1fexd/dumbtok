@file:SuppressLint("UnsafeOptInUsageError")
package fe.dumbtok.module.tiktok

import android.annotation.SuppressLint
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import fe.dumbtok.module.media3.KtorHttpDataSource
import io.ktor.client.*

class TikTokStreamFactory(client: HttpClient) {

    val httpDataSourceFactory = KtorHttpDataSource.Factory(client)
    private val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

    fun createSource(url: String): MediaSource {
        return mediaSourceFactory.createMediaSource(MediaItem.fromUri(url))
    }
}
