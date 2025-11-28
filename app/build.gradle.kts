plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.0.0" //Supabase
}

android {
    namespace = "k.nutriguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nutriguard"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // works with Kotlin 2.0.x
    }
}

dependencies {
    // OpenFood API pic rendering
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Supabase Modules
    implementation(platform("io.github.jan-tennert.supabase:bom:2.4.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:VERSION")

    // Supbase KTOR Client (required)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("androidx.compose.foundation:foundation:1.9.5")
    testImplementation("io.ktor:ktor-client-cio:2.3.7")


    // Unit tests (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")



    // (optional but common)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose BOM (manages Compose versions)
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Activity + setContent for Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // ViewModel integration with Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Icons (optional)
    implementation("androidx.compose.material:material-icons-extended")

    // Debug helpers (optional)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room stuff
    val room_version = "2.8.3"

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("io.coil-kt:coil-compose:2.6.0")

    //MLKit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    //CameraX
    val cameraxVersion = "1.3.4"

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:1.4.0") // PreviewView you’re using

}