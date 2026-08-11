package com.canineai.android.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {

    @Test
    fun shouldUseEmulatorFallbackWhenNoConfiguredBaseUrlIsProvided() {
        val baseUrl = ApiConfig.resolveBaseUrl(configuredBaseUrl = null, isEmulator = true)

        assertEquals("http://10.0.2.2:8080/api/v1/", baseUrl)
    }

    @Test
    fun shouldUseConfiguredBaseUrlWhenProvided() {
        val baseUrl = ApiConfig.resolveBaseUrl(
            configuredBaseUrl = "https://api.canineai.example.com/api/v1",
            isEmulator = false
        )

        assertEquals("https://api.canineai.example.com/api/v1/", baseUrl)
    }

    @Test
    fun shouldResolveLanBaseUrlWhenNotEmulator() {
        val baseUrl = ApiConfig.resolveBaseUrl(configuredBaseUrl = null, isEmulator = false)
        assert(baseUrl.contains(":8080/api/v1/"))
        assert(baseUrl.startsWith("http://"))
    }
}
