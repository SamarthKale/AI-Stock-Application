package com.stockpredictor.app.data.remote.api

import com.stockpredictor.app.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

private const val BASE_URL = "https://api.coingecko.com/api/v3/"
private const val API_KEY_HEADER = "x-cg-demo-api-key"

/**
 * Single Retrofit/OkHttp instance for CoinGecko's Demo plan. The API key is read only from
 * [BuildConfig.COINGECKO_API_KEY] (sourced from the gitignored local.properties at build time,
 * see app/build.gradle.kts) — never hardcoded, never logged. The debug-only logging
 * interceptor is capped at BASIC (method/URL/status/duration, no headers or body) specifically
 * so it can never print the key even if a future change bumps the level; [redactHeader] is a
 * second, explicit layer of the same protection.
 *
 * A CoinGecko Demo key is designed for client-side use (low privilege, ~30 calls/min) — unlike
 * the market-data keys CLAUDE.md's general secrets rule was written for — but this still stays
 * behind the same BuildConfig/local.properties seam so a future backend-proxied data source
 * (see CoinDataSource) can replace direct calls without touching call sites.
 */
object RetrofitClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val apiKeyInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader(API_KEY_HEADER, BuildConfig.COINGECKO_API_KEY)
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader(API_KEY_HEADER)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val coinGeckoApi: CoinGeckoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoinGeckoApiService::class.java)
    }
}
