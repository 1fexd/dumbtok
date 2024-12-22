package io.sanghun.compose.video.datasource

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import io.sanghun.compose.video.cache.VideoPlayerCacheManager


@Immutable
data class DefaultDataSourceConfig(
    val createCache: () -> Cache? = { VideoPlayerCacheManager.getCache() },
    val httpDataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory(),
    val dataSourceFactory: (Context, HttpDataSource.Factory) -> DataSource.Factory = { context, httpDataSourceFactory ->
        DefaultDataSource.Factory(context, httpDataSourceFactory)
    },
    val cacheDataSourceFactory: (Cache, DataSource.Factory) -> CacheDataSource.Factory = { cache, dataSourceFactory ->
        CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(dataSourceFactory)
    },
    val mediaSourceFactory: (DataSource.Factory) -> MediaSource.Factory = {
        DefaultMediaSourceFactory(it)
    },
) : DataSourceConfig {

    override fun createMediaSourceFactory(context: Context): MediaSource.Factory {
        val cache = createCache()!!
        val dataSourceFactory = dataSourceFactory(context, httpDataSourceFactory)
        val cacheDataSourceFactory = cacheDataSourceFactory(cache, dataSourceFactory)
        val mediaSourceFactory = mediaSourceFactory(cacheDataSourceFactory)

        return mediaSourceFactory
    }
}

interface DataSourceConfig {
    fun createMediaSourceFactory(context: Context): MediaSource.Factory
}
