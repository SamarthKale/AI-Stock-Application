import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
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

// Phase 6: release signing. keystore.properties (gitignored, same pattern as local.properties)
// holds the real store/key passwords; it and release.keystore.jks are never committed. When
// absent (e.g. a CI job that only needs assembleDebug/compileDebugKotlin, or a fresh checkout
// before the keystore has been provisioned), release builds fall back to the debug signing
// config so the release build type still compiles — that build simply isn't a distributable,
// properly-signed artifact until the real keystore.properties is present.
val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile", "").isNotBlank()

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

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    // Phase 5b: momentum_model.tflite must stay uncompressed in the APK — a compressed asset
    // can't be memory-mapped directly via FileChannel.map, which is how OnDeviceMomentumClassifier
    // loads it.
    androidResources {
        noCompress += "tflite"
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
    // Phase 6: crash diagnostics. mappingFileUploadEnabled defaults to true for the release build
    // type once isMinifyEnabled=true, so de-obfuscated stack traces reach the Firebase console
    // automatically — no separate manual upload step needed.
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    // Regular implementation (not debugImplementation): RetrofitClient references
    // HttpLoggingInterceptor unconditionally from main source, gating only its *log level* on
    // BuildConfig.DEBUG at runtime — a debug-only dependency would fail release compilation.
    implementation(libs.okhttp.logging.interceptor)
    // Phase 5b: on-device momentum classifier. Plain Interpreter artifact (not ML Kit's custom-
    // model API) — the feature vector is 3 floats, no image/text preprocessing helpers needed,
    // and this avoids ML Kit's Play-Services-backed model-download path, which would undermine
    // the guaranteed-offline point of an on-device feature.
    implementation(libs.tensorflow.lite)
    // Phase 5c: exchange map, MapLibre Native Android SDK + OpenFreeMap (see ExchangeMapScreen's
    // doc comment) -- no API key, no manifest placeholder needed (unlike the Google Maps SDK this
    // replaced). Hosted via plain AndroidView, so no separate Compose-interop artifact is needed.
    implementation(libs.maplibre.android.sdk)
    // Practical 7: one-shot FusedLocationProviderClient for Exchange Map's "Locate me" action --
    // see ExchangeMapViewModel's doc comment. No background-location dependency added.
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}