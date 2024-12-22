package fe.dumbtok

import android.app.Application
import fe.dumbtok.extension.koin.androidApplicationContext
import fe.dumbtok.module.preference.PreferenceRepositoryModule
import fe.dumbtok.module.tiktok.TikTokServiceModule
import fe.dumbtok.viewmodel.ViewModelModule
import io.sanghun.compose.video.cache.VideoPlayerCacheManager
import org.koin.core.context.startKoin

class DumbTokApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoPlayerCacheManager.initialize(this, 1024 * 1024 * 1024)

        startKoin {
            androidApplicationContext<DumbTokApp>(this@DumbTokApp)
            modules(
                PreferenceRepositoryModule,
                TikTokServiceModule,
                ViewModelModule
            )
        }
    }
}
