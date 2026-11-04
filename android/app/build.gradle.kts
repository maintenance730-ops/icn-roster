plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CI passes VERSION_CODE (run number) and KS_PASS (keystore password); release.jks is written from a GitHub secret
val vCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
val ksFile = rootProject.file("release.jks")
val ksPass = System.getenv("KS_PASS") ?: ""

android {
    namespace = "com.maintenance730.icnroster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maintenance730.icnroster"
        minSdk = 26
        targetSdk = 34
        versionCode = vCode
        versionName = "1.0.$vCode"
    }

    signingConfigs {
        create("release") {
            if (ksFile.exists() && ksPass.isNotEmpty()) {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = "icnroster"
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksFile.exists() && ksPass.isNotEmpty()) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.webkit:webkit:1.11.0")
}
