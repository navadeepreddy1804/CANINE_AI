package com.canineai.android.util

object PatientIdFormatter {
    fun format(id: String?, hospitalPatientId: String? = null): String {
        if (!hospitalPatientId.isNullOrBlank()) {
            if (hospitalPatientId.startsWith("PT-") && !hospitalPatientId.drop(3).contains("-")) {
                return hospitalPatientId
            }
            val digits = hospitalPatientId.filter { it.isDigit() }
            if (digits.isNotBlank()) {
                return try {
                    String.format("PT-%05d", digits.take(5).toInt())
                } catch (e: Exception) {
                    "PT-$digits"
                }
            }
        }

        if (id.isNullOrBlank()) return "PT-00001"

        if (id.startsWith("PT-")) {
            val suffix = id.removePrefix("PT-")
            if (!suffix.contains("-")) {
                return id
            }
            // UUID after PT- prefix
            val hash = kotlin.math.abs(suffix.hashCode()) % 90000 + 10001
            return "PT-$hash"
        }

        val digits = id.filter { it.isDigit() }
        if (digits.isNotBlank()) {
            return try {
                String.format("PT-%05d", digits.take(5).toInt())
            } catch (e: Exception) {
                "PT-$digits"
            }
        }

        val hash = kotlin.math.abs(id.hashCode()) % 90000 + 10001
        return "PT-$hash"
    }
}
