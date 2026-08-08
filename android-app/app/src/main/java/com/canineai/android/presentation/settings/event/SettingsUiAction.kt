package com.canineai.android.presentation.settings.event

sealed class SettingsUiAction {
    object NavigateBack : SettingsUiAction()
    data class ShowToast(val message: String) : SettingsUiAction()
}
