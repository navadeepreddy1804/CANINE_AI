package com.canineai.android.data.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import retrofit2.Response

object ApiPayloadParser {
    private val gson = Gson()

    fun <T> parse(response: Response<out Any?>, type: Class<T>): T? {
        val body = response.body()
        if (body == null) return null

        val element = when (body) {
            is String -> JsonParser.parseString(body)
            is JsonElement -> body
            else -> gson.toJsonTree(body)
        }

        return if (isWrappedApiResponse(element)) {
            parseWrappedValue(element, type)
        } else {
            gson.fromJson(element, type)
        }
    }

    inline fun <reified T> parse(response: Response<out Any?>): T {
        return parse(response, T::class.java) as T
    }

    private fun isWrappedApiResponse(element: JsonElement): Boolean {
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        return obj.has("success") && obj.has("data")
    }

    private fun <T> parseWrappedValue(element: JsonElement, type: Class<T>): T {
        val obj = element.asJsonObject
        val data = obj.get("data")
        return if (data == null || data.isJsonNull) {
            gson.fromJson(obj, type)
        } else {
            gson.fromJson(data, type)
        }
    }
}
