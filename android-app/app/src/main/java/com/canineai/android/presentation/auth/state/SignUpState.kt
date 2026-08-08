package com.canineai.android.presentation.auth.state

data class SignUpState(
    val fullNameValue: String = "",
    val fullNameError: String? = null,
    val usernameValue: String = "",
    val usernameError: String? = null,
    
    val emailValue: String = "",
    val emailError: String? = null,
    
    val phoneValue: String = "",
    val phoneError: String? = null,
    
    
    val passwordValue: String = "",
    val passwordError: String? = null,
    
    val confirmPasswordValue: String = "",
    val confirmPasswordError: String? = null,
    
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val apiError: String? = null
) {
    val isFormValid: Boolean
        get() = fullNameValue.isNotBlank() && fullNameError == null &&
                emailValue.isNotBlank() && emailError == null &&
                usernameValue.isNotBlank() && usernameError == null &&
                phoneValue.isNotBlank() && phoneError == null &&
                passwordValue.isNotBlank() && passwordError == null &&
                confirmPasswordValue == passwordValue && confirmPasswordError == null
}
