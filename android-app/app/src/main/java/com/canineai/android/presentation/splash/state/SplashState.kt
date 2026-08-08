package com.canineai.android.presentation.splash.state

data class SplashState(
    val loadingMessage: String = "Initializing AI Engine...",
    val progress: Float = 0.0f,
    val navigateTo: NavigationTarget? = null
)

sealed class NavigationTarget {
    object Login : NavigationTarget()
    object Dashboard : NavigationTarget()
}
