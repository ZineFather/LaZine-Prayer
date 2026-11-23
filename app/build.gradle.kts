plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.zine.prayer"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.zine.prayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "4.6.8"
    }
    flavorDimensions += "androidSupport"
    productFlavors {
        create("legacy") {
            dimension = "androidSupport"
            applicationIdSuffix = ".legacy"
            minSdk = 24
            manifestPlaceholders["appLabel"] = "LaZine Prayer • Android 7 Legacy"
        }
        create("modern") {
            dimension = "androidSupport"
            minSdk = 26
            manifestPlaceholders["appLabel"] = "LaZine Prayer"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
}