plugins {
    id("com.android.application")
}

val rahaBundleBuild = providers.gradleProperty("rahaBundleBuild").orNull == "true"

android {
    namespace = "com.raha.browser.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raha.browser.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.4.1"


        vectorDrawables {
            useSupportLibrary = false
        }
    }

    val releaseStoreFile = System.getenv("RAHA_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("RAHA_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RAHA_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RAHA_KEY_PASSWORD")
    val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        if (hasReleaseSigning) {
            create("rahaRelease") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }

        getByName("release") {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("rahaRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Installable, aggressively optimized test APKs. They use the disposable
        // debug certificate, while preserving release R8/resource shrinking.
        create("compact") {
            initWith(getByName("release"))
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-optimized-test"
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("rahaRelease") else signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // Direct-download APKs use ABI splits, while the Play Store AAB uses
    // ndk.abiFilters. AGP must not see both mechanisms in the same build.
    // The workflow therefore invokes APK and AAB builds separately and passes
    // -PrahaBundleBuild=true only for bundleRelease.
    if (rahaBundleBuild) {
        defaultConfig {
            ndk {
                abiFilters += setOf("arm64-v8a", "armeabi-v7a")
            }
        }
    } else {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "armeabi-v7a")
                isUniversalApk = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Compress native libraries inside direct-download APKs. Android extracts
        // them on install; Google Play still performs device-specific delivery.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    // Pinned for reproducible builds. Review GeckoView releases before public releases.
    implementation("org.mozilla.geckoview:geckoview:150.0.20260511200624")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
}
