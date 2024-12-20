import fe.buildsrc.Version
import fe.buildsrc.dependency.Grrfe

plugins {
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    id("net.nemerosa.versioning")
}

android {
    namespace = "fe.dumbtok"
    compileSdk = Version.COMPILE_SDK

    defaultConfig {
        applicationId = "fe.dumbtok"
        minSdk = Version.MIN_SDK
        targetSdk = Version.COMPILE_SDK
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "DumbTok Debug")
        }

        release {
            isMinifyEnabled = true
            resValue("string", "app_name", "DumbTok")
        }
    }

    kotlin {
        jvmToolchain(Version.JVM)
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/atomicfu.kotlin_module")
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
    implementation("io.ktor:ktor-client-okhttp-jvm:_")
    implementation(AndroidX.lifecycle.runtime.ktx)
    implementation(AndroidX.activity.compose)
    implementation(platform(AndroidX.compose.bom))
    implementation(AndroidX.compose.ui)
    implementation(AndroidX.compose.ui.graphics)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.compose.material3)
    implementation(AndroidX.media3.exoPlayer)
    implementation(AndroidX.media3.ui)
    implementation(AndroidX.media3.session)
    implementation(AndroidX.media3.exoPlayer.dash)
    implementation(AndroidX.media3.dataSource.okhttp)

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
