package com.canineai.android.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("canineai_session", Context.MODE_PRIVATE)

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String,
        fullName: String,
        role: String?,                                        // nullable — new accounts have no roleTitle yet
        loginTimestamp: Long = System.currentTimeMillis(),
        deviceInfo: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
        sessionExpiration: Long = System.currentTimeMillis() + (3600 * 1000)
    ) {
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("email", email)
            putString("full_name", fullName)
            putString("role", role.orEmpty())                 // stored as "" when null; UI shows "Clinician"
            putLong("login_timestamp", loginTimestamp)
            putString("device_info", deviceInfo)
            putLong("session_expiration", sessionExpiration)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun getEmail(): String? = prefs.getString("email", null)

    fun getFullName(): String? = prefs.getString("full_name", null)

    fun getRole(): String? = prefs.getString("role", null)

    fun getLoginTimestamp(): Long = prefs.getLong("login_timestamp", 0L)

    fun getDeviceInfo(): String? = prefs.getString("device_info", null)

    fun getSessionExpiration(): Long = prefs.getLong("session_expiration", 0L)

    fun getServerUrl(): String? = prefs.getString("server_url", null)

    fun saveServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
    }

    fun clearSession() {
        val serverUrl = getServerUrl()
        prefs.edit().clear().apply()
        if (serverUrl != null) {
            saveServerUrl(serverUrl)
        }
    }

    fun hasActiveSession(): Boolean {
        val token = getAccessToken()
        val expiration = getSessionExpiration()
        // Allow a 10 seconds margin of safety
        return token != null && System.currentTimeMillis() < (expiration - 10000)
    }
}
