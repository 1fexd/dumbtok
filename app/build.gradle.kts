import fe.buildsrc.Version
import fe.buildsrc.dependency.Grrfe
import fe.buildsrc.dependency._1fexd
import fe.buildsrc.extension.getOrSystemEnv
import fe.buildsrc.extension.readPropertiesOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    id("net.nemerosa.versioning")
}

var appName = "DumbTok"
val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss")

android {
    namespace = "fe.dumbtok"
    compileSdk = Version.COMPILE_SDK

    defaultConfig {
        applicationId = "fe.dumbtok"
        minSdk = Version.MIN_SDK
        targetSdk = Version.COMPILE_SDK

        val now = System.currentTimeMillis()
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of("UTC"))
        val versionInfo = providers.provider { versioning.info }.get()

        versionCode = versionInfo.tag?.let {
            versionInfo.versionNumber.versionCode
        } ?: (now / 1000).toInt()

        versionName = versionInfo.tag ?: versionInfo.full
        val archivesBaseName = if (versionInfo.tag != null) {
            "$appName-$versionName"
        } else "$appName-${dtf.format(localDateTime)}-$versionName"

        setProperty("archivesBaseName", archivesBaseName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        register("env") {
            val properties = rootProject.file(".ignored/keystore.properties").readPropertiesOrNull()

            storeFile = properties.getOrSystemEnv("KEYSTORE_FILE_PATH")?.let { rootProject.file(it) }
            storePassword = properties.getOrSystemEnv("KEYSTORE_PASSWORD")
            keyAlias = properties.getOrSystemEnv("KEY_ALIAS")
            keyPassword = properties.getOrSystemEnv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
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

    kotlin {
        jvmToolchain(Version.JVM)
        compilerOptions {
            freeCompilerArgs.addAll("-P", "plugin:org.jetbrains.kotlin.parcelize:experimentalCodeGeneration=true")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"

        }
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
    implementation("com.gitlab.grrfe:jsoup-ext:_")

    implementation(Grrfe.ext.gson)

    implementation(Koin.android)
    implementation(Koin.compose)
    implementation(_1fexd.android.preference.core)
    implementation(_1fexd.android.preference.compose)
    implementation(_1fexd.android.preference.composeMock)
    implementation(_1fexd.android.compose.dialog)
    implementation(_1fexd.android.compose.route)
    implementation(_1fexd.android.span.compose)
    implementation(_1fexd.android.lifecycleUtil.core)
    implementation(_1fexd.android.lifecycleUtil.koin)
    implementation(_1fexd.composeKit.app.core)
    implementation(_1fexd.composeKit.theme.core)
    implementation(_1fexd.composeKit.theme.preference)
    implementation(_1fexd.composeKit.component)
    implementation(_1fexd.composeKit.core)
    implementation(_1fexd.composeKit.layout)

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

    testImplementation(JetBrains.ktor.client.core)
    testImplementation(JetBrains.ktor.client.mock)

    testImplementation("com.willowtreeapps.assertk:assertk:_")
    testImplementation(kotlin("test"))
    androidTestImplementation(platform(AndroidX.compose.bom))
    androidTestImplementation(AndroidX.compose.ui.testJunit4)
    debugImplementation(AndroidX.compose.ui.tooling)
    debugImplementation(AndroidX.compose.ui.testManifest)
}
