package com.canineai.android.presentation.settings.event

sealed class SettingsEvent {
    // Profile Updates
    data class FullNameChanged(val value: String) : SettingsEvent()
    data class PhoneChanged(val value: String) : SettingsEvent()
    data class HospitalChanged(val value: String) : SettingsEvent()
    data class DepartmentChanged(val value: String) : SettingsEvent()
    data class RoleChanged(val value: String) : SettingsEvent()
    data class MedicalRegNoChanged(val value: String) : SettingsEvent()
    object SaveProfileClicked : SettingsEvent()
    object DismissSuccessDialog : SettingsEvent()

    // Application Configuration
    data class DarkModeToggled(val value: Boolean) : SettingsEvent()
    data class NotificationsToggled(val value: Boolean) : SettingsEvent()
    data class LanguageChanged(val value: String) : SettingsEvent()

    // Storage Actions
    object CleanTemporaryFiles : SettingsEvent()

    // Security Actions
    data class LogoutDevice(val deviceName: String) : SettingsEvent()
    object LogoutAllDevices : SettingsEvent()
}
