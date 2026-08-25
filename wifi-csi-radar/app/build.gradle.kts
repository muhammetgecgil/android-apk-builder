plugins {
    id("com.android.application")
}

android {
    namespace = "com.muhammetgecgil.wifiradar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.muhammetgecgil.wifiradar"
        minSdk = 26
        targetSdk = 36
        versionCode = 50001
        versionName = "5.0.0"
    }

    val ksFile = System.getenv("WIFI_RADAR_KEYSTORE")
    val ksPass = System.getenv("WIFI_RADAR_STORE_PASSWORD")
    val keyAliasEnv = System.getenv("WIFI_RADAR_KEY_ALIAS")
    val keyPass = System.getenv("WIFI_RADAR_KEY_PASSWORD")

    signingConfigs {
        if (!ksFile.isNullOrBlank() && !ksPass.isNullOrBlank() && !keyAliasEnv.isNullOrBlank() && !keyPass.isNullOrBlank()) {
            create("play") {
                storeFile = file(ksFile)
                storePassword = ksPass
                keyAlias = keyAliasEnv
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.findByName("play") != null) {
                signingConfig = signingConfigs.getByName("play")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}
