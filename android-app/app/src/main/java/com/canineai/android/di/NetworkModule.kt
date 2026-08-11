package com.canineai.android.di

import com.canineai.android.data.network.ApiConfig
import com.canineai.android.data.network.ApiResponse
import com.canineai.android.data.network.AuthInterceptor
import com.canineai.android.data.network.CanineApiService
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.Response
import java.io.File
import java.io.IOException

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        sessionManager: com.canineai.android.data.local.SessionManager,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: com.canineai.android.data.network.TokenAuthenticator
    ): OkHttpClient {
        val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
        val cache = Cache(File(context.cacheDir, "offline_cache"), cacheSize)

        val hostSelectionInterceptor = Interceptor { chain ->
            var request = chain.request()
            val customUrl = sessionManager.getServerUrl()
            if (!customUrl.isNullOrBlank()) {
                try {
                    val normalized = ApiConfig.normalizeBaseUrl(customUrl)
                    val newHttpUrl = normalized.toHttpUrlOrNull()
                    if (newHttpUrl != null) {
                        val newUrl = request.url.newBuilder()
                            .scheme(newHttpUrl.scheme)
                            .host(newHttpUrl.host)
                            .port(newHttpUrl.port)
                            .build()
                        request = request.newBuilder().url(newUrl).build()
                    }
                } catch (e: Exception) {
                    // fallback to original request
                }
            }
            chain.proceed(request)
        }

        val offlineInterceptor = Interceptor { chain ->
            var request = chain.request()
            if (!isNetworkAvailable(context)) {
                val cacheControl = okhttp3.CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()
                request = request.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .cacheControl(cacheControl)
                    .build()
            }
            chain.proceed(request)
        }

        val networkInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = okhttp3.CacheControl.Builder()
                .maxAge(1, TimeUnit.MINUTES)
                .build()
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor(offlineInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(networkInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    @Provides
    @Singleton
    fun provideCanineApiService(
        okHttpClient: OkHttpClient,
        sessionManager: com.canineai.android.data.local.SessionManager
    ): CanineApiService {
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.DEVICE.startsWith("emulator")

        val baseUrl = ApiConfig.resolveBaseUrl(
            configuredBaseUrl = sessionManager.getServerUrl(),
            isEmulator = isEmulator
        )

        val gson = GsonBuilder()
            .registerTypeAdapterFactory(object : TypeAdapterFactory {
                override fun <T : Any?> create(gson: com.google.gson.Gson, type: TypeToken<T>): TypeAdapter<T>? {
                    if (type.rawType != ApiResponse::class.java) {
                        return null
                    }

                    val dataType = (type.type as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
                        ?: Any::class.java

                    return object : TypeAdapter<T>() {
                        override fun write(out: JsonWriter, value: T?) {
                            if (value == null) {
                                out.nullValue()
                                return
                            }

                            val response = value as ApiResponse<*>
                            val obj = com.google.gson.JsonObject()
                            obj.addProperty("success", response.success)
                            response.message?.let { obj.addProperty("message", it) }
                            response.data?.let { obj.add("data", gson.toJsonTree(it)) }
                            out.jsonValue(obj.toString())
                        }

                        override fun read(`in`: JsonReader): T? {
                            val element = JsonParser.parseReader(`in`)
                            if (!element.isJsonObject) {
                                return gson.fromJson(element, type.type) as T?
                            }

                            val obj = element.asJsonObject
                            if (obj.has("success") && (obj.has("data") || obj.has("message"))) {
                                val dataValue: Any? = if (obj.has("data") && !obj.get("data").isJsonNull) {
                                    gson.fromJson<Any>(obj.get("data"), TypeToken.get(dataType).type)
                                } else {
                                    null
                                }
                                val response = ApiResponse(
                                    success = obj.get("success").asBoolean,
                                    message = obj.get("message")?.takeIf { !it.isJsonNull }?.asString,
                                    data = dataValue
                                )
                                @Suppress("UNCHECKED_CAST")
                                return response as T
                            }

                            val dataValue: Any? = gson.fromJson<Any>(element, TypeToken.get(dataType).type)
                            val fallback = ApiResponse(success = true, message = null, data = dataValue)
                            @Suppress("UNCHECKED_CAST")
                            return fallback as T
                        }
                    }
                }
            })
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CanineApiService::class.java)
    }
}
