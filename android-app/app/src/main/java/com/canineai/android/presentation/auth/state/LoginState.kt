package com.canineai.android.presentation.auth.state

data class LoginState(
    val emailValue: String = "",
    val emailError: String? = null,
    val passwordValue: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isRememberMeChecked: Boolean = false,
    val isLoading: Boolean = false,
    val apiError: String? = null,
    val isOffline: Boolean = false,
    val isBiometricsAvailable: Boolean = false
) {
    val isFormValid: Boolean
        get() = emailValue.isNotBlank() && 
                emailError == null && 
                passwordValue.isNotBlank() && 
                passwordError == null
}
