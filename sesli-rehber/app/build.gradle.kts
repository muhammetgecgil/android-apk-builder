plugins {
    id("com.android.application")
}

android {
    namespace = "com.mgecgil.seslirehber"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mgecgil.seslirehber"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "0.20.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        noCompress += "tflite"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.1")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mediapipe:tasks-vision:0.10.29")
    implementation("com.google.ai.edge.litert:litert:2.1.0")
    implementation("com.google.ar:core:1.54.0")
    testImplementation("junit:junit:4.13.2")
}
