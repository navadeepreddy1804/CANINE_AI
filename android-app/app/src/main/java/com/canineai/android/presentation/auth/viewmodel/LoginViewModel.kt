package com.canineai.android.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.network.LoginRequest
import com.canineai.android.data.network.NetworkErrorResolver
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.auth.event.LoginEvent
import com.canineai.android.presentation.auth.event.LoginUiAction
import com.canineai.android.presentation.auth.state.LoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _uiActions = Channel<LoginUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> updateEmail(event.email)
            is LoginEvent.PasswordChanged -> updatePassword(event.password)
            is LoginEvent.TogglePasswordVisibility -> togglePasswordVisibility()
            is LoginEvent.ToggleRememberMe -> toggleRememberMe()
            is LoginEvent.SubmitLogin -> performLogin()
            is LoginEvent.SubmitGoogleLogin -> performGoogleLogin(event.idToken)
            is LoginEvent.BiometricAuthRequested -> { /* Biometrics disabled for this release */ }
            is LoginEvent.DismissError -> _state.update { it.copy(apiError = null) }
        }
    }

    private fun updateEmail(email: String) {
        val error = when {
            email.isBlank() -> "Email address is required"
            !EMAIL_PATTERN.matches(email) -> "Invalid email address format"
            else -> null
        }
        _state.update { it.copy(emailValue = email, emailError = error, apiError = null) }
    }

    private fun updatePassword(password: String) {
        val error = when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must contain at least 6 characters"
            else -> null
        }
        _state.update { it.copy(passwordValue = password, passwordError = error, apiError = null) }
    }

    private fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun toggleRememberMe() {
        _state.update { it.copy(isRememberMeChecked = !it.isRememberMeChecked) }
    }

    private fun performLogin() {
        if (!_state.value.isFormValid) return

        _state.update { it.copy(isLoading = true, apiError = null) }

        viewModelScope.launch {
            try {
                repository.login(
                    LoginRequest(
                        email = _state.value.emailValue,
                        password = _state.value.passwordValue
                    )
                )
                _state.update { it.copy(isLoading = false) }
                // Traditional login doesn't have isProfileComplete in this legacy logic, assuming it's complete or handled later.
                // Or maybe we should check it? But user asked to focus on Google OAuth.
                _uiActions.send(LoginUiAction.NavigateToDashboard)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        apiError = NetworkErrorResolver.resolve(e)
                    )
                }
            }
        }
    }

    private fun performGoogleLogin(idToken: String) {
        _state.update { it.copy(isLoading = true, apiError = null) }
        viewModelScope.launch {
            try {
                val loginResponse = repository.googleLogin(
                    com.canineai.android.data.network.GoogleAuthRequest(idToken = idToken)
                )
                _state.update { it.copy(isLoading = false) }
                
                if (loginResponse.user != null && !loginResponse.user.profileComplete) {
                    _uiActions.send(LoginUiAction.NavigateToCompleteProfile)
                } else {
                    _uiActions.send(LoginUiAction.NavigateToDashboard)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        apiError = NetworkErrorResolver.resolve(e)
                    )
                }
            }
        }
    }
}
