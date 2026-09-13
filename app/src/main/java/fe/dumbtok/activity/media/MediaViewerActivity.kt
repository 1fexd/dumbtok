@file:SuppressLint("UnsafeOptInUsageError")
package fe.dumbtok.activity.media

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.MediaSource
import fe.composekit.appbase.AppBaseComponentActivity
import fe.composekit.appbase.AppTheme
import fe.dumbtok.composable.MediaViewer
import fe.dumbtok.composable.theme.AppColor
import fe.dumbtok.composable.theme.Typography
import fe.dumbtok.module.tiktok.ImagePost
import fe.dumbtok.module.tiktok.Post
import fe.dumbtok.module.tiktok.VideoPost
import fe.dumbtok.viewmodel.MediaViewerViewModel
import io.sanghun.compose.video.datasource.DefaultDataSourceConfig
import org.koin.androidx.viewmodel.ext.android.viewModel

class MediaViewerActivity : AppBaseComponentActivity() {
    private val viewModel by viewModel<MediaViewerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MediaViewerActivity", "onCreate")


        val post = intent.extras?.getParcelable<Post>("post")
        Log.d("MediaViewerActivity", "$post")

        if (post == null) {
            return
        }

        val dataSourceConfig = DefaultDataSourceConfig(httpDataSourceFactory = viewModel.service.httpDataSourceFactory)
        val mediaSourceFactory = dataSourceConfig.createMediaSourceFactory(this)

        setContent(edgeToEdge = true) {
            AppTheme(
                appColor = AppColor,
                typography = Typography,
                theme = viewModel.theme(),
                materialYou = viewModel.themeMaterialYou(),
                amoled = viewModel.themeAmoled()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    val mediaSources = remember<List<MediaSource>?>(post) {
                        return@remember when (post) {
                            is ImagePost -> createImagePostMediaSource(mediaSourceFactory, post)
                            is VideoPost -> listOf(
                                mediaSourceFactory.createMediaSource(
                                    MediaItem.Builder().setUri(Uri.parse(post.videoUrl)).build()
                                )
                            )

                            else -> null
                        }
                    }

                    if (mediaSources != null) {
                        MediaViewer(mediaSources = mediaSources)
                    }
                }
            }
        }
    }
}
