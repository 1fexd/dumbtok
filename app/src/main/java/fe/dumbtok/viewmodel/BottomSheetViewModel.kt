package fe.dumbtok.viewmodel

import androidx.lifecycle.ViewModel
import fe.composekit.theme.preference.ThemePreferences
import fe.dumbtok.module.preference.app.AppPreferenceRepository
import fe.dumbtok.module.tiktok.TikTokService

class BottomSheetViewModel(
     private val preferenceRepository: AppPreferenceRepository,
     val service: TikTokService,
) : ViewModel() {
     val theme = preferenceRepository.asState(ThemePreferences.theme)
     val themeMaterialYou = preferenceRepository.asState(ThemePreferences.themeMaterialYou)
     val themeAmoled = preferenceRepository.asState(ThemePreferences.themeAmoled)
}
