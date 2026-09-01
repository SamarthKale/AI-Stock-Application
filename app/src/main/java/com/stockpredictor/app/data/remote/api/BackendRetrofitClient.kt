package com.stockpredictor.app.data.remote.api

import com.stockpredictor.app.BuildConfig
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.net.BackendUrlResolver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Separate Retrofit/OkHttp instance from [RetrofitClient] (CoinGecko) — different base URL
 * (the Spring Boot backend, resolved via [BackendUrlResolver] — see its doc comment for the
 * Debug emulator-vs-physical-device split; Release is a pure pass-through to
 * [BuildConfig.BACKEND_BASE_URL]) and different auth
 * (Firebase ID token Bearer header, not a CoinGecko API key). This is the first real consumer
 * of "Android calls Spring Boot" — Phase 4 never needed it since market data goes straight to
 * CoinGecko.
 *
 * Token refresh follows CLAUDE.md's original Phase 4 guidance for this exact seam: the
 * interceptor uses the non-forcing `getIdToken(false)` in the common path, and [authenticator]
 * force-refreshes only after an observed 401, retrying at most once.
 */
object BackendRetrofitClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val authRepository = FirebaseAuthRepository()

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val token = runBlocking { authRepository.getIdToken(forceRefresh = false) }
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val authenticator = Authenticator { _, response ->
        if (responseChainLength(response) >= 2) return@Authenticator null // already retried once
        if (response.request.header("Authorization") == null) return@Authenticator null // never authenticated to begin with
        val freshToken = runBlocking { authRepository.getIdToken(forceRefresh = true) } ?: return@Authenticator null
        response.request.newBuilder().header("Authorization", "Bearer $freshToken").build()
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS) // model inference can be slower than a quote lookup
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .addInterceptor(loggingInterceptor)
        .build()

    val backendApi: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BackendUrlResolver.resolve())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApiService::class.java)
    }
}
