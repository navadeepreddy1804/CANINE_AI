package com.canineai.android.presentation.auth.event

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object TogglePasswordVisibility : LoginEvent()
    object ToggleRememberMe : LoginEvent()
    object SubmitLogin : LoginEvent()
    data class SubmitGoogleLogin(val idToken: String) : LoginEvent()
    object BiometricAuthRequested : LoginEvent()
    object DismissError : LoginEvent()
}
