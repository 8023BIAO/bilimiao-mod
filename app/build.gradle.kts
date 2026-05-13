import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.a10miaomiao.bilimiao"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.a10miaomiao.bilimiao.mod"
        minSdk = 21
        targetSdk = 36
        versionCode = 16
        versionName = "2026.05.14-05"

        flavorDimensions("default")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 保留全ABI
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    val signingFile = file("signing.properties")
    if (signingFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(signingFile))
        signingConfigs {
            create("miao") {
                keyAlias = props.getProperty("KEY_ALIAS")
                keyPassword = props.getProperty("KEY_PASSWORD")
                storeFile = file(props.getProperty("KEYSTORE_FILE"))
                storePassword = props.getProperty("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "bilimiao dev")
            manifestPlaceholders["channel"] = "Development"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // 跳过 lintVital，节省大量时间
            lint { checkReleaseBuilds = false }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 使用debug签名（指纹A9:7D...）
            signingConfig = signingConfigs.getByName("debug")
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    productFlavors {
        create("foss") {
            dimension = flavorDimensionList[0]
            manifestPlaceholders["channel"] = "FOSS"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kodein.di) // 依赖注入

    implementation(libs.kongzue.dialogx) {
        exclude("com.github.kongzue.DialogX", "DialogXInterface")
    }
    implementation(libs.materialkolor)

//    implementation("com.github.li-xiaojun:XPopup:2.9.13")
//    implementation("com.github.lihangleo2:ShadowLayout:3.2.4")

    implementation(libs.splitties.android.base)
    implementation(libs.splitties.android.base.with.views.dsl)
    implementation(libs.splitties.android.appcompat)
    implementation(libs.splitties.android.appcompat.with.views.dsl)
    implementation(libs.splitties.android.material.components)
    implementation(libs.splitties.android.material.components.with.views.dsl)

    implementation(libs.mojito)
    implementation(libs.mojito.sketch)
    implementation(libs.mojito.glide)

    // 播放器相关
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.gsy.video.player)

    implementation(libs.okhttp3)
    implementation(libs.pbandk.runtime)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.microg.safeparcel)

    implementation(project(":bilimiao-comm"))
    implementation(project(":bilimiao-download"))
    implementation(project(":bilimiao-cover"))
//    implementation project(":bilimiao-appwidget")
    implementation(project(":bilimiao-compose"))
    // 弹幕引擎
    implementation(project(":DanmakuFlameMaster"))



    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}