import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Read local.properties directly rather than via Gradle's project properties, so the key never
// needs to be passed as a -P flag or environment variable that could end up in shell history/CI
// logs — the file itself is already gitignored (see .gitignore) and is never committed.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val coinGeckoApiKey: String = localProperties.getProperty("COINGECKO_API_KEY", "")
// Phase 5: Spring Boot backend, for real predictions. 10.0.2.2 is the standard Android emulator
// alias for the host machine's localhost — override in local.properties (BACKEND_BASE_URL=...)
// for a physical device (LAN IP) or a deployed backend later.
val backendBaseUrl: String = localProperties.getProperty("BACKEND_BASE_URL", "http://10.0.2.2:8080/")

android {
    namespace = "com.stockpredictor.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.stockpredictor.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Never hardcoded in source — read from local.properties (gitignored) at build time.
        // BuildConfig fields are compiled into the APK as plain constants like any other app
        // code; that's expected for a Demo-tier client key (see RetrofitClient's doc comment).
        buildConfigField("String", "COINGECKO_API_KEY", "\"$coinGeckoApiKey\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    // Regular implementation (not debugImplementation): RetrofitClient references
    // HttpLoggingInterceptor unconditionally from main source, gating only its *log level* on
    // BuildConfig.DEBUG at runtime — a debug-only dependency would fail release compilation.
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}