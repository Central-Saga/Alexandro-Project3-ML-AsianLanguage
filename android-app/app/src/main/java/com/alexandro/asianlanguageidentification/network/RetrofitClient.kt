package com.alexandro.asianlanguageidentification.network

import com.alexandro.asianlanguageidentification.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interceptor pintar yang otomatis mencoba 10.0.2.2 (IP host standar emulator Android)
 * jika koneksi ke 127.0.0.1 gagal (atau sebaliknya).
 */
class HostFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalUrl = request.url

        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            val host = originalUrl.host
            val fallbackHost = when (host) {
                "127.0.0.1", "localhost" -> "10.0.2.2"
                "10.0.2.2" -> "127.0.0.1"
                else -> null
            }
            if (fallbackHost != null) {
                val newUrl = originalUrl.newBuilder()
                    .host(fallbackHost)
                    .build()
                val newRequest = request.newBuilder()
                    .url(newUrl)
                    .build()
                return chain.proceed(newRequest)
            }
            throw e
        }
    }
}

object RetrofitClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val authenticatedRequest = if (
                    request.url.encodedPath.endsWith("/feedback") &&
                    BuildConfig.FEEDBACK_API_TOKEN.isNotEmpty()
                ) {
                    request.newBuilder()
                        .header("X-Feedback-Token", BuildConfig.FEEDBACK_API_TOKEN)
                        .build()
                } else {
                    request
                }
                chain.proceed(authenticatedRequest)
            }
            .addInterceptor(HostFallbackInterceptor())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val api: LanguageApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LanguageApi::class.java)
    }
}
