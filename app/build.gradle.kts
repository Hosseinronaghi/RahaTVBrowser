plugins {
    id("com.android.application")
}

android {
    namespace = "com.raha.browser.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raha.browser.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.3.2"

        vectorDrawables {
            useSupportLibrary = false
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }

        getByName("release") {
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
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // GeckoView ships as a multi-architecture AAR. Per-ABI APKs remove the
    // unused native engines and provide the largest practical APK reduction.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
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
}
