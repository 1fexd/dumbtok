package fe.dumbtok.composable

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.source.MediaSource
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.datasource.DataSourceConfig
import io.sanghun.compose.video.datasource.DefaultDataSourceConfig


@Composable
fun MediaViewer(
    dataSourceConfig: DataSourceConfig = DefaultDataSourceConfig(),
    mediaSources: List<MediaSource>,
) {
    VideoPlayer(
        mediaSource = mediaSources,
        dataSourceConfig = dataSourceConfig,
        handleLifecycle = true,
        autoPlay = true,
        usePlayerController = true,
        enablePip = false,
        handleAudioFocus = true,
        controllerConfig = VideoPlayerControllerConfig(
            showSpeedAndPitchOverlay = false,
            showSubtitleButton = false,
            showCurrentTimeAndTotalTime = true,
            showBufferingProgress = false,
            showForwardIncrementButton = true,
            showBackwardIncrementButton = true,
            showBackTrackButton = true,
            showNextTrackButton = true,
            showRepeatModeButton = true,
            showFullScreenButton = true,
            controllerShowTimeMilliSeconds = 5_000,
            controllerAutoShow = true,
        ),
        volume = 0.5f,  // volume 0.0f to 1.0f
        repeatMode = RepeatMode.NONE,       // or RepeatMode.ALL, RepeatMode.ONE
        onCurrentTimeChanged = { // long type, current player time (millisec)
            Log.e("CurrentTime", it.toString())
        },
        playerInstance = { // ExoPlayer instance (Experimental)

        },
        modifier = Modifier.fillMaxSize()
    )
}
