package fe.dumbtok.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fe.dumbtok.composable.VideoPlayer
import fe.dumbtok.storage.AndroidPrivateStorageBackend
import fe.dumbtok.tiktok.TikTokService
import fe.dumbtok.tiktok.TiktokEvent
import fe.dumbtok.tiktok.TiktokPostMedia
import fe.dumbtok.tiktok.TiktokStatus
import fe.dumbtok.tiktok.TiktokVideoPost
import fe.dumbtok.ui.theme.dumbtokTheme
import io.ktor.client.engine.okhttp.*

class LinkHandlerActivity : ComponentActivity() {
    private val tiktokApiService = TikTokService(
        engine = OkHttp.create(),
        storage = AndroidPrivateStorageBackend(this)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.dataString

        enableEdgeToEdge()
        setContent {
            dumbtokTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    EventList(url = url)
                }
            }
        }
    }

    @Composable
    private fun EventList(url: String?) {
        val event by tiktokApiService.events.collectAsStateWithLifecycle()
        val events = remember { mutableStateListOf<TiktokEvent>() }

        var post by remember {
            mutableStateOf<TiktokPostMedia?>(null)
        }

        LaunchedEffect(key1 = url) {
            if (url == null) return@LaunchedEffect

            val status = tiktokApiService.extractPost(url = url)
            if (status is TiktokStatus.Success) {
                post = status.post
            }
        }

        LaunchedEffect(key1 = event) {
            Log.d("dumbtok", "$event")
            events.add(event)
        }

        Text(text = "Event: $event, count: ${events.size}")


        LazyColumn {
            items(items = events) { event ->
                Text(text = "$event")
            }
        }

        if (post is TiktokVideoPost) {
            val videoSource = remember(post) {
                tiktokApiService.stream(post as TiktokVideoPost)
            }

            VideoPlayer(source = videoSource)
        }
    }
}
