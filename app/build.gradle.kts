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
        versionCode = 6
        versionName = "0.2.4"

        vectorDrawables {
            useSupportLibrary = false
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
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
    // Pinned for reproducible builds. Review GeckoView releases before every public release.
    implementation("org.mozilla.geckoview:geckoview:150.0.20260511200624")
    implementation("androidx.annotation:annotation:1.9.1")
}
