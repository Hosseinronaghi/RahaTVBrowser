plugins {
    id("com.android.application")
}

val bundleBuild = providers.gradleProperty("rahaBundleBuild").orNull == "true"
val signingEnabled = System.getenv("RAHA_SIGNING_ENABLED") == "true"

android {
    namespace = "com.raha.browser.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raha.browser.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 18
        versionName = "0.6.0"

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (signingEnabled) {
            create("rahaRelease") {
                storeFile = file(System.getenv("RAHA_KEYSTORE_FILE"))
                storePassword = System.getenv("RAHA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RAHA_KEY_ALIAS")
                keyPassword = System.getenv("RAHA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        create("compact") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (signingEnabled) signingConfigs.getByName("rahaRelease") else signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingEnabled) signingConfig = signingConfigs.getByName("rahaRelease")
        }
    }

    if (!bundleBuild) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "armeabi-v7a")
                isUniversalApk = true
            }
        }
    } else {
        defaultConfig {
            ndk { abiFilters += setOf("arm64-v8a", "armeabi-v7a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            assets.srcDir("src/main/res/font")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.documentfile:documentfile:1.1.0")

    val media3 = "1.8.0"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
