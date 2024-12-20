package fe.dumbtok.tiktok

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import fe.dumbtok.media3.KtorHttpDataSource
import io.ktor.client.*

class TikTokStreamFactory(client: HttpClient) {

    private val dataSourceFactory = KtorHttpDataSource.Factory(client)
    private val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    fun createSource(url: String): MediaSource {
        return mediaSourceFactory.createMediaSource(MediaItem.fromUri(url))
    }
}
