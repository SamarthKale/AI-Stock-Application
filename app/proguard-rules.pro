# Phase 6 — Production Hardening. Verified against a real assembleRelease build with
# isMinifyEnabled=true, isShrinkResources=true (not just "should work in theory").

# --- kotlinx.serialization ---
# Retrofit's CoinGecko DTOs (data/remote/api/dto/CoinGeckoDtos.kt) and every model/ class that's
# ever encoded/decoded (kotlinx.serialization.json) are reflection-driven via each class's
# generated $serializer companion — R8 can strip these as "unused" without an explicit keep,
# which fails silently at runtime (a SerializationException on the first real network response),
# not at compile time. Rules follow kotlinx.serialization's own documented consumer-proguard
# recommendation.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.stockpredictor.app.**$$serializer { *; }
-keepclassmembers class com.stockpredictor.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.stockpredictor.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit / OkHttp ---
# Retrofit's interface method signatures carry generic type info (Call<CoinDto>, etc.) that R8's
# default optimizations can erase; Retrofit's own consumer rules normally cover this, but keeping
# the service interface explicitly is cheap insurance since it's this app's only Retrofit surface.
-keep interface com.stockpredictor.app.data.remote.api.** { *; }
-keepattributes Signature, Exceptions

# --- TensorFlow Lite (Phase 5b on-device momentum classifier) ---
# The plain Interpreter API (not ML Kit) still uses JNI/reflection internally for delegate
# selection; TFLite ships its own consumer rules, but this keep is defense-in-depth for the one
# feature whose Definition of Done explicitly requires working correctly under R8 (airplane-mode
# on-device classification must survive obfuscation, not just compile).
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# --- Google Maps Compose (Phase 5c exchange map) ---
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# --- Firebase (Auth / Firestore / Messaging) ---
# FirestoreSyncRepository writes/reads plain Map<String, Any> documents (verified — no
# reflection-mapped POJOs in this codebase), so no @PropertyName model-class keep is structurally
# needed today. Kept anyway as defense-in-depth against a future POJO being added without
# updating this file, and because Firebase's Auth/Messaging internals still use reflection for
# some provider/service discovery paths that this rule protects unconditionally.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
