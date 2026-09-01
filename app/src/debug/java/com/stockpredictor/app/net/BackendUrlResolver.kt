package com.stockpredictor.app.net

import android.os.Build
import com.stockpredictor.app.BuildConfig

/**
 * Debug-only backend URL resolution. Exists ONLY in this source set --
 * app/src/release/java/.../net/BackendUrlResolver.kt is a trivial pass-through with the exact
 * same `resolve()` signature, so [com.stockpredictor.app.data.remote.api.BackendRetrofitClient]
 * (main source) compiles against either one, but a Release build never contains this
 * emulator/device-detection logic at all -- not "disabled," genuinely absent from that APK.
 *
 * Solves the emulator-vs-physical-device split without ever touching `local.properties`:
 * [BuildConfig.BACKEND_BASE_URL] still comes from Gradle exactly as before (default
 * `http://10.0.2.2:8080/`, overridable in `local.properties`). If it's still that compiled
 * default, this picks the right loopback address at *runtime* based on what the app is actually
 * installed on. If a developer explicitly overrode it (a LAN IP, a future deployed host), that
 * value always wins unchanged -- auto-detection only ever fills in the two known-good local
 * addresses, never second-guesses a deliberate override.
 */
object BackendUrlResolver {
    private const val EMULATOR_DEFAULT = "http://10.0.2.2:8080/"

    // Requires `adb reverse tcp:8080 tcp:8080` to be active on the connected physical device --
    // that command makes the device's own "localhost:8080" reach this machine's backend, the
    // same way 10.0.2.2 is the emulator's built-in alias for it.
    private const val PHYSICAL_DEVICE_URL = "http://127.0.0.1:8080/"

    fun resolve(): String {
        val configured = BuildConfig.BACKEND_BASE_URL
        if (configured != EMULATOR_DEFAULT) return configured // explicit override -- always respected
        return if (isRunningOnEmulator()) EMULATOR_DEFAULT else PHYSICAL_DEVICE_URL
    }

    /**
     * Standard `Build.*` fingerprint heuristics for "is this an emulator" -- no single field is
     * reliable across every emulator image/vendor, so this checks the common markers together
     * (covers the AVD images this project has actually been tested on, e.g. a `sdk_gphone*`
     * product/model, plus Genymotion and generic goldfish/ranchu images).
     */
    private fun isRunningOnEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val product = Build.PRODUCT
        val hardware = Build.HARDWARE
        val brand = Build.BRAND
        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && product.startsWith("sdk")) ||
            product.contains("sdk_gphone") ||
            hardware == "goldfish" ||
            hardware == "ranchu"
    }
}
