package com.canineai.android.data.network

import com.canineai.android.BuildConfig

object ApiConfig {
    private const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080/api/v1"
    private const val DEFAULT_PRODUCTION_BASE_URL = "https://api.canineai.example.com/api/v1"

    fun resolveBaseUrl(
        configuredBaseUrl: String? = null,
        isEmulator: Boolean = false
    ): String {
        if (!configuredBaseUrl.isNullOrBlank()) {
            return normalizeBaseUrl(configuredBaseUrl)
        }

        if (isEmulator) {
            return normalizeBaseUrl(DEFAULT_EMULATOR_BASE_URL)
        }

        val configUrl = BuildConfig.API_BASE_URL
        if (configUrl.isNotBlank() && !configUrl.contains("10.0.2.2") && !configUrl.contains("localhost") && !configUrl.contains("127.0.0.1") && !configUrl.contains("canineai.example.com")) {
            return normalizeBaseUrl(configUrl)
        }

        val devIp = BuildConfig.DEVELOPER_IP
        if (devIp.isNotBlank() && devIp != "10.0.2.2") {
            return normalizeBaseUrl("http://$devIp:8080/api/v1")
        }

        return normalizeBaseUrl(DEFAULT_PRODUCTION_BASE_URL)
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().removeSuffix("/")
        return if (trimmed.endsWith("/api/v1")) {
            "$trimmed/"
        } else {
            "$trimmed/api/v1/"
        }
    }
}
