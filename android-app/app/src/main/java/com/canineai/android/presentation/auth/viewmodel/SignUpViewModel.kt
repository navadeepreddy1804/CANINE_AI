package com.canineai.android.presentation.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.network.LoginRequest
import com.canineai.android.data.network.NetworkErrorResolver
import com.canineai.android.data.network.RegisterRequest
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.auth.event.SignUpEvent
import com.canineai.android.presentation.auth.event.SignUpUiAction
import com.canineai.android.presentation.auth.state.SignUpState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    private val _uiActions = Channel<SignUpUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.FullNameChanged -> updateFullName(event.fullName)
            is SignUpEvent.UsernameChanged -> updateUsername(event.username)
            is SignUpEvent.EmailChanged -> updateEmail(event.email)
            is SignUpEvent.PhoneChanged -> updatePhone(event.phone)
            
            is SignUpEvent.PasswordChanged -> updatePassword(event.password)
            is SignUpEvent.ConfirmPasswordChanged -> updateConfirmPassword(event.confirmPassword)
            is SignUpEvent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is SignUpEvent.DismissError -> _state.update { it.copy(apiError = null) }
            is SignUpEvent.SubmitSignUp -> performSignUp()
        }
    }

    private fun updateFullName(name: String) {
        val error = if (name.isBlank()) "Full name is required" else null
        _state.update { it.copy(fullNameValue = name, fullNameError = error, apiError = null) }
    }

    private fun updateUsername(username: String) {
        val error = if (username.isBlank()) "Username is required" else null
        _state.update { it.copy(usernameValue = username, usernameError = error, apiError = null) }
    }

    private fun updateEmail(email: String) {
        val error = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email address format"
            else -> null
        }
        _state.update { it.copy(emailValue = email, emailError = error, apiError = null) }
    }

    private fun updatePhone(phone: String) {
        val error = if (phone.isBlank()) "Phone number is required" else null
        _state.update { it.copy(phoneValue = phone, phoneError = error, apiError = null) }
    }

    

    private fun updatePassword(pass: String) {
        val error = when {
            pass.isBlank() -> "Password is required"
            pass.length < 8 -> "Password must contain at least 8 characters"
            else -> null
        }
        _state.update { it.copy(passwordValue = pass, passwordError = error, apiError = null) }
    }

    private fun updateConfirmPassword(confirm: String) {
        val error = if (confirm != _state.value.passwordValue) "Passwords do not match" else null
        _state.update { it.copy(confirmPasswordValue = confirm, confirmPasswordError = error, apiError = null) }
    }

    private fun performSignUp() {
        if (!_state.value.isFormValid) return

        _state.update { it.copy(isLoading = true, apiError = null) }

        viewModelScope.launch {
            try {
                // 1. Register — saves the user to MySQL
                repository.register(
                    RegisterRequest(
                        email    = _state.value.emailValue,
                        password = _state.value.passwordValue,
                        fullName = _state.value.fullNameValue,
                        phone    = _state.value.phoneValue.ifBlank { null },
                        username = _state.value.usernameValue,
                        securityQuestion = "What is your primary clinical department?",
                        securityAnswer = "Orthodontics"
                    )
                )

                // 2. Auto-login to establish the session immediately after signup
                repository.login(
                    LoginRequest(
                        email    = _state.value.emailValue,
                        password = _state.value.passwordValue
                    )
                )

                _state.update { it.copy(isLoading = false) }
                _uiActions.send(SignUpUiAction.NavigateToDashboard)

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, apiError = resolveSignUpError(e))
                }
            }
        }
    }

    /**
     * Maps registration exceptions to specific, actionable messages.
     *
     * The backend's GlobalExceptionHandler returns conflict errors as HTTP 409
     * with a JSON body whose `message` field is one of:
     *   - "Email address is already registered"
     *   - "Username is already in use"
     *
     * NetworkErrorResolver.resolve() parses that body first, so the message
     * that arrives here is already the backend's sentence. We pattern-match
     * against it to pick the most precise wording for the UI.
     */
    private fun resolveSignUpError(e: Exception): String {
        val base = NetworkErrorResolver.resolve(e)
        val lower = base.lowercase()
        return when {
            lower.contains("email") && (lower.contains("registered") || lower.contains("already") || lower.contains("exists"))
                -> "That email address is already registered. Please sign in or use a different email."
            lower.contains("username") && (lower.contains("use") || lower.contains("already") || lower.contains("exists") || lower.contains("taken"))
                -> "That username is already taken. Please choose a different one."
            lower.contains("password")
                -> "Password must be at least 8 characters and include uppercase, lowercase, and a digit."
            lower.contains("phone")
                -> "Invalid phone number format. Please check and try again."
            else -> base
        }
    }
}
