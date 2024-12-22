package fe.dumbtok.activity.media

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import fe.dumbtok.module.tiktok.ImagePost

data class ItemPosition(
    val start: Long,
    val end: Long,
    val duration: Long,
    val isLast: Boolean,
)

fun createImagePostMediaSource(mediaSourceFactory: MediaSource.Factory, post: ImagePost): List<MediaSource> {
    val fullDuration = post.music?.duration?.times(1000)?.toLong() ?: (post.imageUrls.size * 5000L)
    val durationPerImageMs = (fullDuration.toDouble() / post.imageUrls.size).toLong()

    val sources = mutableListOf<Pair<MediaSource, ItemPosition>>()

    for ((i, url) in post.imageUrls.withIndex()) {
        val start = i * durationPerImageMs
        val isLast = i == post.imageUrls.size - 1

        val end = if (isLast) fullDuration else start + durationPerImageMs

        Log.d(
            "MediaViewerActivity",
            "i=$i, musicDuration=$fullDuration: start=$start, end=$end, durationPerImageMs=$durationPerImageMs"
        )

        val duration = end - start

        val position = ItemPosition(start, start + duration, duration, isLast)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setImageDurationMs(duration)
            .build()

        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
        sources.add(mediaSource to position)
    }

    var musicMediaItemSource: MediaSource? = null
    if(post.music != null) {
        val musicMediaItem = MediaItem.Builder().setUri(Uri.parse(post.music.url)).build()
        musicMediaItemSource = mediaSourceFactory.createMediaSource(musicMediaItem)
    }

    val concatenatingMusicSource = ConcatenatingMediaSource2.Builder().setMediaSourceFactory(mediaSourceFactory)
    val concatenatingMediaSource = ConcatenatingMediaSource2.Builder().setMediaSourceFactory(mediaSourceFactory)

    for ((source, position) in sources) {
        val (start, end, duration, isLast) = position

        Log.d("MediaViewerActivity", "adding source, ${source.mediaItem}, position=$position")
        val startUs = Util.msToUs(start)
        val endUs = Util.msToUs(end)

        if(musicMediaItemSource != null){
            Log.d("MediaViewerActivity", "Clipping to startUs=$startUs, endUs=$endUs")
            val clippedMusicSource = ClippingMediaSource(musicMediaItemSource, startUs, endUs)
            concatenatingMusicSource.add(clippedMusicSource, duration)
        }

        concatenatingMediaSource.add(source, duration)
    }

    val concatenatedMediaSource = concatenatingMediaSource.build()
    if(post.music == null) {
        return listOf(concatenatedMediaSource)
    }

    val concatenatedMusicSource = concatenatingMusicSource.build()

    val merging = MergingMediaSource(
        true,
        false,
        concatenatedMediaSource, concatenatedMusicSource
    )

    return listOf(merging)
}
