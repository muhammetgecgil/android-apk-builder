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
        versionCode = 7
        versionName = "0.6.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
