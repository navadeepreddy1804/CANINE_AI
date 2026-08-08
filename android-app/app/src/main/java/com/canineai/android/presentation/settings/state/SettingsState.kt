package com.canineai.android.presentation.settings.state

data class SettingsState(
    // Profile values are loaded from the authenticated backend account.
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val hospital: String = "",
    val department: String = "",
    val medicalRegNo: String = "",
    val isDarkMode: Boolean = false,
    val language: String = "English",
    val isNotificationsEnabled: Boolean = true,
    val dateAndTimeFormat: String = "",
    val sessionTimeoutMin: Int? = null,
    val currentAiEngine: String = "",
    val inferenceTimeoutSec: Int? = null,
    val footerText: String = "",
    val disclaimerText: String = "",
    // The backend does not expose storage or device-session data yet.
    val studiesSizeGb: Float? = null,
    val reportsSizeGb: Float? = null,
    val logsSizeGb: Float? = null,
    val maxStorageGb: Float? = null,
    val devices: List<DeviceSession> = emptyList(),
    val isSaving: Boolean = false,
    val showSaveSuccess: Boolean = false,
    val apiError: String? = null
)

data class DeviceSession(
    val deviceName: String,
    val clientType: String,
    val lastActive: String,
    val isCurrent: Boolean
)
