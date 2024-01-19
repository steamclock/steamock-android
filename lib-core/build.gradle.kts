import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.21"
}

android {
    namespace = "com.steamclock.steamock.lib"
    compileSdk = 34

    // Postman mocking setup pulled from local.properties
    val localProps = gradleLocalProperties(rootDir)
    val postmanAccessKey: String = localProps.getProperty("postmanAccessKey")
    val postmanMockCollectionId: String = localProps.getProperty("postmanCollectionId")
    val postmanMockServerUrl: String = localProps.getProperty("postmanMockServerUrl")

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "postmanAccessKey", postmanAccessKey)
        buildConfigField("String", "postmanMockCollectionId", postmanMockCollectionId)
        buildConfigField("String", "postmanMockServerUrl", postmanMockServerUrl)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        // This is tied to our Kotlin version.
        kotlinCompilerExtensionVersion = "1.5.2"
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Data serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    // Basic network calls (Postman APIs)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-coroutines:5.0.0-alpha.12")

//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.5")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}