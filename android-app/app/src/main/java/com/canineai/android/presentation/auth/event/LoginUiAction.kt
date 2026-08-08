package com.canineai.android.presentation.auth.event

sealed class LoginUiAction {
    object NavigateToDashboard : LoginUiAction()
    object NavigateToCompleteProfile : LoginUiAction()
    data class ShowToastError(val message: String) : LoginUiAction()
}
