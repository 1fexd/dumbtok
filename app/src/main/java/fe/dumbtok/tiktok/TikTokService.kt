package fe.dumbtok.tiktok

import androidx.annotation.VisibleForTesting
import androidx.media3.exoplayer.source.MediaSource
import com.google.gson.JsonObject
import fe.dumbtok.extension.followRedirects
import fe.dumbtok.storage.StorageBackend
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TikTokService(
    private val storage: StorageBackend,
    engine: HttpClientEngine = OkHttp.create(),
    private val httpClient: HttpClient = TikTokHttpClient(engine),
    private val streamFactory: TikTokStreamFactory = TikTokStreamFactory(httpClient),
    private val extractor: TiktokMediaExtractor = TiktokMediaExtractor(),
    private val urlService: TikTokUrlService = TikTokUrlService(),
) {
    private val _events = MutableStateFlow<TiktokEvent>(TiktokEvent.Idle)
    val events = _events.asStateFlow()

    @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    suspend fun resolveShortLink(url: String): String? {
        val response = httpClient.get(urlString = url)

        val location = response.headers[HttpHeaders.Location] ?: return null
        val (_, _, postId) = urlService.isFullUrl(location) ?: return null
        return postId
    }

    private suspend fun resolveFullPostUrl(url: String): String? {
        if (urlService.isShortUrl(url)) {
            return resolveShortLink(url)?.let { urlService.formatPostUrl(it) }
        }

        val (_, _, postId) = urlService.isFullUrl(url) ?: return null
        return urlService.formatPostUrl(postId)
    }

    private suspend fun fetchPostDetail(url: String): JsonObject? {
        val response = httpClient.followRedirects(urlString = url) { get(urlString = it) }
        return response.bodyAsChannel().toInputStream().use { extractor.parseHtmlPage(it, url) }
    }

//    private suspend fun downloadMediaFile(fileName: String, url: String): String? {
//        val response = client.followRedirects(urlString = url) { get(urlString = it) }
//        if (!response.status.isSuccess()) return null
//
//        response.bodyAsChannel().toInputStream().use { videoStream ->
//            storage.create(fileName).use { outputStream ->
//                videoStream.copyTo(outputStream)
//            }
//        }
//
//        return fileName
//    }

    suspend fun extractPost(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        url: String,
    ): TiktokStatus? = withContext(dispatcher) {
        _events.emit(TiktokEvent.Resolving)
        val postUrl = resolveFullPostUrl(url) ?: return@withContext TiktokStatus.ResolveFailure

        _events.emit(TiktokEvent.Fetching)
        val result = fetchPostDetail(postUrl) ?: return@withContext TiktokStatus.FetchDetailFailure

        _events.emit(TiktokEvent.Extracting)
        val post = extractor.extract(result) ?: return@withContext TiktokStatus.ExtractFailure

//        when (post) {
//            is TiktokVideoPost -> {
//                downloadMediaFile(post.postId + ".mp4", post.videoUrl)
//            }
//
//            is TiktokImagePost -> {
//                for ((idx, imageUrl) in post.imageUrls.withIndex()) {
//                    downloadMediaFile(post.postId + "_$idx.jpeg", imageUrl)
//                }
//            }
//        }

        return@withContext TiktokStatus.Success(post)
    }

    fun stream(post: TiktokVideoPost): MediaSource {
        return streamFactory.createSource(post.videoUrl)
    }
}

sealed interface TiktokEvent {
    data object Idle : TiktokEvent
    data object Resolving : TiktokEvent
    data object Fetching : TiktokEvent
    data object Extracting : TiktokEvent
    data object Downloading : TiktokEvent
}

sealed interface TiktokStatus {
    data object ResolveFailure : TiktokStatus
    data object FetchDetailFailure : TiktokStatus
    data object ExtractFailure : TiktokStatus
    data class Success(val post: TiktokPostMedia) : TiktokStatus
}
