package fe.dumbtok.module.tiktok

import androidx.annotation.VisibleForTesting
import com.google.gson.JsonObject
import fe.dumbtok.extension.followRedirects
import fe.dumbtok.module.storage.AndroidPrivateStorageBackend
import fe.dumbtok.module.storage.StorageBackend
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.dsl.module

val TikTokServiceModule = module {
    single<TikTokService> { TikTokService(storage = AndroidPrivateStorageBackend(get())) }
}

class TikTokService(
    private val storage: StorageBackend,
    engine: HttpClientEngine = OkHttp.create(),
    private val httpClient: HttpClient = TikTokHttpClient(engine),
    private val streamFactory: TikTokStreamFactory = TikTokStreamFactory(httpClient),
    private val extractor: TiktokMediaExtractor = TiktokMediaExtractor(),
    private val urlService: TikTokUrlService = TikTokUrlService(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val httpDataSourceFactory = streamFactory.httpDataSourceFactory

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

    suspend fun extractPost(url: String): TikTokStatus = withContext(dispatcher) {
        _events.emit(TiktokEvent.Resolving)
        val postUrl = resolveFullPostUrl(url) ?: return@withContext TikTokStatus.ResolveFailure

        _events.emit(TiktokEvent.Fetching)
        val result = fetchPostDetail(postUrl) ?: return@withContext TikTokStatus.FetchDetailFailure

        _events.emit(TiktokEvent.Extracting)
        val post = extractor.extract(result) ?: return@withContext TikTokStatus.ExtractFailure

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

        return@withContext TikTokStatus.Success(post)
    }

//    fun stream(post: TiktokVideoPost): MediaSource {
//        return streamFactory.createSource(post.videoUrl)
//    }
}

sealed interface TiktokEvent {
    data object Idle : TiktokEvent
    data object Resolving : TiktokEvent
    data object Fetching : TiktokEvent
    data object Extracting : TiktokEvent
    data object Downloading : TiktokEvent
}

sealed interface TikTokStatus {
    data object ResolveFailure : TikTokStatus
    data object FetchDetailFailure : TikTokStatus
    data object ExtractFailure : TikTokStatus
    data class Success(val post: Post) : TikTokStatus
}
