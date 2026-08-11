package com.canineai.android.data.network

import com.canineai.android.BuildConfig

object ApiConfig {
    private const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080/api/v1"
    private const val DEFAULT_LAN_BASE_URL = "http://10.37.23.120:8080/api/v1"

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

        val devIp = BuildConfig.DEVELOPER_IP
        if (devIp.isNotBlank() && devIp != "10.0.2.2" && devIp != "localhost") {
            return normalizeBaseUrl("http://$devIp:8080/api/v1")
        }

        return normalizeBaseUrl(DEFAULT_LAN_BASE_URL)
    }

    fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().removeSuffix("/")
        return if (trimmed.endsWith("/api/v1")) {
            "$trimmed/"
        } else {
            "$trimmed/api/v1/"
        }
    }
}
