package fe.dumbtok.activity.bottomsheet

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import fe.composekit.appbase.AppBaseComponentActivity
import fe.composekit.appbase.AppTheme
import fe.dumbtok.activity.media.MediaViewerActivity
import fe.dumbtok.composable.component.bottomsheet.ImprovedBottomDrawer
import fe.dumbtok.composable.component.bottomsheet.LoadingIndicator
import fe.dumbtok.composable.theme.AppColor
import fe.dumbtok.composable.theme.Typography
import fe.dumbtok.module.tiktok.TikTokStatus
import fe.dumbtok.viewmodel.BottomSheetViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class BottomSheetActivity : AppBaseComponentActivity() {
    private val viewModel by viewModel<BottomSheetViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.data.toString()

        setContent(edgeToEdge = true) {
            AppTheme(
                appColor = AppColor,
                typography = Typography,
                theme = viewModel.theme(),
                materialYou = viewModel.themeMaterialYou(),
                amoled = viewModel.themeAmoled()
            ) { Wrapper(url = url) }
        }
    }

    private fun start(status: TikTokStatus.Success) {
        val intent = Intent(this, MediaViewerActivity::class.java)
        intent.putExtra("post", status.post)

        startActivity(intent)
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Wrapper(url: String) {
        var status by remember { mutableStateOf<TikTokStatus?>(null) }
        val drawerState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()

        val hideDrawer: () -> Unit = {
            coroutineScope.launch { drawerState.hide() }.invokeOnCompletion { finish() }
        }

        LaunchedEffect(key1 = url) {
            status = viewModel.service.extractPost(url = url)

            if (status is TikTokStatus.Success) {
                hideDrawer()
                start(status as TikTokStatus.Success)
            }
        }

        val configuration = LocalConfiguration.current
        val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isBlackTheme = viewModel.themeAmoled()

        ImprovedBottomDrawer(
            landscape = landscape,
            isBlackTheme = isBlackTheme,
            drawerState = drawerState,
            shape = RoundedCornerShape(
                topStart = 22.0.dp,
                topEnd = 22.0.dp,
                bottomEnd = 0.0.dp,
                bottomStart = 0.0.dp
            ),
            hide = hideDrawer,
            sheetContent = {
                SheetContainer {
                    if (status == null) {
                        LoadingIndicator(events = viewModel.service.events)
                    }
                }
            }
        )
    }

    @Composable
    fun SheetContainer(content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .fillMaxWidth()
                .padding(start = 5.dp, end = 5.dp, bottom = 10.dp), content = content
        )
    }
}
