package com.canineai.android.data.network

import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized network exception → user-readable message mapper.
 *
 * Resolution order:
 *   1. If it is an HttpException, parse the backend's ApiResponse JSON body
 *      and return the `message` field directly — it is already a friendly
 *      sentence written by GlobalExceptionHandler.
 *   2. If the body is absent or unparseable, map by HTTP status code.
 *   3. For non-HTTP exceptions, map by exception type.
 *   4. Generic fallback for anything unrecognised.
 */
object NetworkErrorResolver {

    fun resolve(throwable: Throwable): String = when (throwable) {
        is HttpException   -> resolveHttp(throwable)
        is UnknownHostException -> "Network unavailable. Please check your internet connection."
        is ConnectException,
        is NoRouteToHostException -> "Cannot reach the server. Make sure the backend is running and your device is on the same Wi-Fi network."
        is SocketTimeoutException -> "Connection timed out. The server took too long to respond. Try again."
        else -> extractMessageOrFallback(throwable)
    }

    // ── HTTP errors ──────────────────────────────────────────────────────────

    private fun resolveHttp(ex: HttpException): String {
        // First try to parse the backend ApiResponse body
        val bodyMessage = extractBodyMessage(ex)
        if (!bodyMessage.isNullOrBlank()) {
            return bodyMessage
        }

        // Fallback: map by status code
        return when (ex.code()) {
            400 -> "Invalid request. Please check your input and try again."
            401 -> "Incorrect email or password."
            403 -> "You do not have permission to perform this action."
            404 -> "The requested resource was not found."
            409 -> "A conflict occurred. The email or username may already be in use."
            422 -> "Validation failed. Please review the form fields."
            in 500..599 -> "A server error occurred. Please try again in a moment."
            else -> "Unexpected error (HTTP ${ex.code()}). Please try again."
        }
    }

    /**
     * Extracts the `message` field from the backend's ApiResponse JSON envelope:
     *   { "success": false, "message": "...", "data": null }
     * Returns null if the body is missing, empty, or not valid JSON.
     */
    private fun extractBodyMessage(ex: HttpException): String? {
        return try {
            val body = ex.response()?.errorBody()?.string()
            if (body.isNullOrBlank()) return null
            val json = JSONObject(body)
            json.optString("message", "").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    // ── Non-HTTP errors ──────────────────────────────────────────────────────

    /**
     * For exceptions that carry a readable message (e.g. those thrown by
     * CanineRepository.handleResponse when success=false), return the message
     * directly if it looks like a sentence, otherwise use the generic fallback.
     */
    private fun extractMessageOrFallback(throwable: Throwable): String {
        val msg = throwable.message ?: return genericFallback()

        // If the message looks like a sentence rather than a class name / stack
        // trace fragment, return it directly.
        if (msg.length in 8..200 && !msg.contains("Exception") && !msg.contains("at ")) {
            return msg
        }
        return genericFallback()
    }

    private fun genericFallback() =
        "Something went wrong. Please try again."
}
