package fe.dumbtok.composable

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import io.sanghun.compose.video.datasource.DataSourceConfig
import io.sanghun.compose.video.datasource.DefaultDataSourceConfig
import io.sanghun.compose.video.uri.toUri


typealias MediaItemBuilder = MediaItem.Builder.() -> Unit

interface BasePlayerMediaItem {
    val mediaMetadata: MediaMetadata
    val mimeType: String
    val mediaItemBuilder: MediaItemBuilder
}

fun VideoPlayerMediaItem.createMediaItem(context: Context): MediaItem {
    val uri = toUri(context)
    return MediaItem.Builder()
        .apply {
            setUri(uri)
            setMediaMetadata(mediaMetadata)
            setMimeType(mimeType)
            setDrmConfiguration(
                if (this@createMediaItem is VideoPlayerMediaItem.NetworkMediaItem) {
                    drmConfiguration
                } else {
                    null
                },
            )
        }
        .apply(mediaItemBuilder)
        .build()
}


fun createExoPlayer(
    context: Context,
    dataSourceConfig: DataSourceConfig = DefaultDataSourceConfig(),
    seekBeforeMilliSeconds: Long = 10000L,
    seekAfterMilliSeconds: Long = 10000L,
    handleAudioFocus: Boolean = true,
    playerBuilder: ExoPlayer.Builder.() -> ExoPlayer.Builder = { this },
): ExoPlayer {
    val mediaSourceFactory = dataSourceConfig.createMediaSourceFactory(context)

    return ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(seekBeforeMilliSeconds)
        .setSeekForwardIncrementMs(seekAfterMilliSeconds)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            handleAudioFocus,
        )
//        .setLoadControl(DefaultLoadControl.Builder()
//            .shouldContinueLoading
//            .build())
        .setMediaSourceFactory(mediaSourceFactory)
        .playerBuilder()
        .build()
//        .also(playerInstance)
}
