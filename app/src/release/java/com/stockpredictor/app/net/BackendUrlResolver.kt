package com.stockpredictor.app.net

import com.stockpredictor.app.BuildConfig

/**
 * Release counterpart of the debug-only BackendUrlResolver (see that file's doc comment for the
 * full design). A pure pass-through, zero emulator/device-detection logic -- Release's backend
 * URL behavior is byte-for-byte identical to what [BuildConfig.BACKEND_BASE_URL] already was
 * before this resolver existed: no development address, no hardcoded production hostname.
 */
object BackendUrlResolver {
    fun resolve(): String = BuildConfig.BACKEND_BASE_URL
}
