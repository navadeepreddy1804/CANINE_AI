package com.canineai.android.presentation.splash.event

sealed class SplashEvent {
    object StartInitialization : SplashEvent()
    object CompleteAnimation : SplashEvent()
}
