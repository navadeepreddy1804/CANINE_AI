package com.canineai.android.presentation.auth.event

sealed class SignUpEvent {
    data class FullNameChanged(val fullName: String) : SignUpEvent()
    data class UsernameChanged(val username: String) : SignUpEvent()
    data class EmailChanged(val email: String) : SignUpEvent()
    data class PhoneChanged(val phone: String) : SignUpEvent()
    
    data class PasswordChanged(val password: String) : SignUpEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent()
    object TogglePasswordVisibility : SignUpEvent()
    object SubmitSignUp : SignUpEvent()
    object DismissError : SignUpEvent()
}
