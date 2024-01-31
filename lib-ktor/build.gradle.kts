plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

//============================================================
// Maven/Jitpack Publishing
//============================================================
android {
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}


afterEvaluate {
    publishing {
        publications {
            val release by publications.registering(MavenPublication::class) {
                from(components["release"])
                artifactId = "lib-ktor"
                groupId = "com.github.steamclock.steamock-android"
                version = "1.0"
            }
        }
    }
}
//============================================================

android {
    namespace = "com.steamclock.steamock.lib_ktor"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":lib-core"))
    // Networking (ktor)
    // https://ktor.io/docs/migrating-2.html#testing-api
    implementation("io.ktor:ktor-client-core:2.3.6")
}