package com.canineai.android.presentation.auth.event

sealed class SignUpUiAction {
    object NavigateToDashboard : SignUpUiAction()
    data class ShowToastError(val message: String) : SignUpUiAction()
}
