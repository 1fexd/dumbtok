package fe.dumbtok.module.preference


import fe.dumbtok.module.preference.app.AppPreferenceRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val PreferenceRepositoryModule = module {
    singleOf(::AppPreferenceRepository)
}
