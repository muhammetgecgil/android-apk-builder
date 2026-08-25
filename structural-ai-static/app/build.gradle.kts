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
        versionCode = 10
        versionName = "0.9.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
