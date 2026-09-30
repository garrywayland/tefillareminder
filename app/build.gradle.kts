plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.garry.reminder"
    compileSdk = 34
    defaultConfig { applicationId = "com.garry.reminder"; minSdk = 30; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation("com.kosherjava:zmanim:2.5.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
}
