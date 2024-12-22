package fe.dumbtok.viewmodel

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val ViewModelModule = module {
    viewModelOf(::BottomSheetViewModel)
    viewModelOf(::MediaViewerViewModel)
}
