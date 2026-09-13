import com.gitlab.grrfe.gradlebuild.Version
import com.gitlab.grrfe.gradlebuild.android.AndroidSdk
import com.gitlab.grrfe.gradlebuild.android.ArchiveBaseName
import com.gitlab.grrfe.gradlebuild.android.version.DefaultFallbackVersionCodeProducer
import com.gitlab.grrfe.gradlebuild.android.version.SemverProducer
import com.gitlab.grrfe.gradlebuild.android.version.VersionCodeProducer
import com.gitlab.grrfe.gradlebuild.android.version.createAndroidVersionProvider
import com.gitlab.grrfe.gradlebuild.common.CompilerOption
import com.gitlab.grrfe.gradlebuild.common.KotlinCompilerArgs
import com.gitlab.grrfe.gradlebuild.common.PluginOption
import com.gitlab.grrfe.gradlebuild.util.PropertiesFile
import com.gitlab.grrfe.gradlebuild.util.SystemEnvironment
import com.gitlab.grrfe.gradlebuild.util.withProviders
import fe.build.dependencies.Grrfe
import fe.build.dependencies._1fexd
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    id("com.gitlab.grrfe.android-build-plugin")
}

var appName = "DumbTok"
object NightlyTagVersionCodeProducer : VersionCodeProducer {
    private fun readResolve(): Any = NightlyTagVersionCodeProducer
    private val DTF: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val NIGHTLY_TAG_REGEX = Regex("^nightly-(\\d{4})(\\d{2})(\\d{2})(\\d{2})$")

    override fun produceVersionCode(tag: String): Int? {
        println("Handling nightly tag $tag")
        val match = NIGHTLY_TAG_REGEX.matchEntire(tag)?.groupValues ?: return null

        val (_, year, month, day, buildNum) = match
        val date = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        val dateStr = date.format(DTF) + buildNum.padStart(1, '0')

        return dateStr.toIntOrNull()
    }
}

android {
    namespace = "fe.dumbtok"
    compileSdk = 37

    defaultConfig {

        applicationId = "fe.linksheet"
        minSdk = AndroidSdk.MIN_SDK
        targetSdk = AndroidSdk.COMPILE_SDK

        val now = System.currentTimeMillis()

        val versionProvider = createAndroidVersionProvider(
            versionCodeProducer = { tag ->
                NightlyTagVersionCodeProducer.produceVersionCode(tag) ?: SemverProducer.produceVersionCode(tag)
            },
            fallbackVersionCodeProducer = DefaultFallbackVersionCodeProducer
        )
        val (name, code, commit, branch) = versionProvider.get()
        versionCode = code
        versionName = name

        with(ArchiveBaseName) {
            project.base.setArchivesName(appName, name, now)
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testOptions.unitTests.isIncludeAndroidResources = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        register("env") {
            val properties = with(PropertiesFile) {
                rootProject.file(".ignored/keystore.properties").readPropertiesOrNull()
            }
            val provider = withProviders(properties, SystemEnvironment)
            storeFile = provider.get("KEYSTORE_FILE_PATH")?.let { rootProject.file(it) }
            storePassword = provider.get("KEYSTORE_PASSWORD")
            keyAlias = provider.get("KEY_ALIAS")
            keyPassword = provider.get("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "$appName Debug")
        }

        release {
            isMinifyEnabled = true
            resValue("string", "app_name", appName)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        register("nightly") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks.add("release")
            signingConfig = signingConfigs.getByName("env")

            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly"

            resValue("string", "app_name", "$appName Nightly")
        }
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"

        }
    }
}

kotlin {
    jvmToolchain(Version.JVM)
    with(compilerOptions.freeCompilerArgs) {
        addAll(KotlinCompilerArgs.createCompilerOptions(CompilerOption.SkipPreReleaseCheck))
        addAll(KotlinCompilerArgs.createPluginOptions(PluginOption.Parcelize.ExperimentalCodeGeneration to true))
    }
}

dependencies {
    implementation(platform(AndroidX.compose.bom))
    implementation(AndroidX.compose.ui)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.compose.material3)

    implementation(AndroidX.compose.material.icons.core)
    implementation(AndroidX.compose.material.icons.extended)
    implementation(AndroidX.activity.compose)

    implementation(AndroidX.core.ktx)
    implementation(AndroidX.compose.animation)
    implementation(AndroidX.navigation.compose)

    implementation(AndroidX.lifecycle.process)
    implementation(AndroidX.lifecycle.runtime.compose)
    implementation(AndroidX.lifecycle.viewModelCompose)
    implementation(AndroidX.lifecycle.runtime.ktx)

    implementation(AndroidX.webkit)
    implementation(AndroidX.browser)

    implementation(JetBrains.ktor.client.core)
    implementation(JetBrains.ktor.client.gson)
    implementation(JetBrains.ktor.client.okHttp)
    implementation(JetBrains.ktor.client.android)
    implementation(JetBrains.ktor.client.encoding)
    implementation(JetBrains.ktor.client.mock)
    implementation(JetBrains.ktor.client.logging)
    implementation("com.gitlab.grrfe:jsoup-ext:_")

    implementation(platform(Grrfe.gsonExt.bom))
    implementation(Grrfe.gsonExt.core)

    implementation(Koin.android)
    implementation(Koin.compose)
    implementation(platform(_1fexd.composeKit.bom))
    implementation(_1fexd.composeKit.core)
    implementation(_1fexd.composeKit.preference.core)
    implementation(_1fexd.composeKit.preference.compose.core2)
    implementation(_1fexd.composeKit.preference.compose.core)
    implementation(_1fexd.composeKit.preference.compose.mock)
    implementation(_1fexd.composeKit.lifecycle.core)
    implementation(_1fexd.composeKit.lifecycle.koin)
    implementation(_1fexd.composeKit.span.compose)
    implementation(_1fexd.composeKit.compose.dialog)
    implementation(_1fexd.composeKit.compose.route)
    implementation(_1fexd.composeKit.compose.app)
    implementation(_1fexd.composeKit.compose.theme.core)
    implementation(_1fexd.composeKit.compose.theme.preference)
    implementation(_1fexd.composeKit.compose.component)
    implementation(_1fexd.composeKit.compose.layout)

    implementation("io.ktor:ktor-client-okhttp-jvm:_")
    implementation(AndroidX.lifecycle.runtime.ktx)
    implementation(AndroidX.activity.compose)
    implementation(AndroidX.activity)

    implementation(platform(AndroidX.compose.bom))
    implementation(AndroidX.compose.ui)
    implementation(AndroidX.compose.ui.graphics)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.compose.material3)
    implementation(AndroidX.media3.common)
    implementation(AndroidX.media3.database)
    implementation(AndroidX.media3.dataSource)
    implementation(AndroidX.media3.exoPlayer)
    implementation(AndroidX.media3.session)
    androidTestImplementation(AndroidX.media3.testUtils)
    androidTestImplementation(AndroidX.media3.testUtils.robolectric)
    implementation(AndroidX.media3.ui)


    implementation(AndroidX.appCompat)
    implementation(Google.android.material)

    testImplementation(Koin.test)
    testImplementation(Koin.junit4)
    testImplementation(Koin.android)
    testImplementation(Testing.junit4)
    testImplementation(Testing.robolectric)
    testImplementation(KotlinX.coroutines.test)
    testImplementation(CashApp.turbine)

    testImplementation(JetBrains.ktor.client.core)
    testImplementation(JetBrains.ktor.client.mock)

    testImplementation("com.willowtreeapps.assertk:assertk:_")
    testImplementation(kotlin("test"))
    testImplementation(Testing.robolectric)
    testImplementation(AndroidX.test.ext.junit.ktx)
    androidTestImplementation(platform(AndroidX.compose.bom))
    androidTestImplementation(AndroidX.compose.ui.testJunit4)
    debugImplementation(AndroidX.compose.ui.tooling)
    debugImplementation(AndroidX.compose.ui.testManifest)
}
