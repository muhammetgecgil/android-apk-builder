plugins {
    id("com.android.application")
}

android {
    namespace = "com.mg.structuralai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mg.structuralai"
        minSdk = 26
        targetSdk = 35
        versionCode = 45
        versionName = "1.12.0-boundary-load-gate"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
