package com.canineai.android.data.network

import com.canineai.android.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiServiceProvider: Provider<CanineApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If the request itself was an attempt to authenticate/refresh and failed with 401, give up.
        if (response.request.url.encodedPath.contains("auth/refresh") ||
            response.request.url.encodedPath.contains("auth/login")) {
            return null
        }

        synchronized(this) {
            val currentToken = sessionManager.getAccessToken()
            
            // If another thread already refreshed the token, retry with the new token
            val headerToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (headerToken != null && currentToken != null && headerToken != currentToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = sessionManager.getRefreshToken() ?: return null

            return try {
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken(mapOf("refreshToken" to refreshToken))
                }

                if (refreshResponse.success && refreshResponse.data != null) {
                    val newAccessToken = refreshResponse.data.accessToken
                    val newRefreshToken = refreshResponse.data.refreshToken
                    
                    sessionManager.saveSession(
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken,
                        email = sessionManager.getEmail().orEmpty(),
                        fullName = sessionManager.getFullName().orEmpty(),
                        role = sessionManager.getRole()
                    )

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    sessionManager.clearSession()
                    null
                }
            } catch (e: Exception) {
                if (e !is java.io.IOException) {
                    sessionManager.clearSession()
                }
                null
            }
        }
    }
}
