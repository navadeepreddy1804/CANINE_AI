package com.canineai.android.presentation.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.local.SessionManager
import com.canineai.android.presentation.splash.event.SplashEvent
import com.canineai.android.presentation.splash.state.NavigationTarget
import com.canineai.android.presentation.splash.state.SplashState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        onEvent(SplashEvent.StartInitialization)
    }

    fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.StartInitialization -> startLoadingTimeline()
            is SplashEvent.CompleteAnimation -> checkAuthenticationAndNavigate()
        }
    }

    private fun startLoadingTimeline() {
        viewModelScope.launch {
            val totalDurationMs = 2500L
            val intervals = 50
            val delayPerInterval = totalDurationMs / intervals

            for (i in 1..intervals) {
                delay(delayPerInterval)
                val progress = i.toFloat() / intervals
                
                val message = when {
                    progress < 0.25f -> "Initializing AI Engine..."
                    progress < 0.50f -> "Loading Security..."
                    progress < 0.75f -> "Preparing Platform..."
                    else -> "Almost Ready..."
                }

                _state.update { it.copy(progress = progress, loadingMessage = message) }
            }
            onEvent(SplashEvent.CompleteAnimation)
        }
    }

    private fun checkAuthenticationAndNavigate() {
        viewModelScope.launch {
            delay(200)
            val isAuthenticated = sessionManager.hasActiveSession()
            
            val target = if (isAuthenticated) {
                NavigationTarget.Dashboard
            } else {
                NavigationTarget.Login
            }
            
            _state.update { it.copy(navigateTo = target) }
        }
    }
}
