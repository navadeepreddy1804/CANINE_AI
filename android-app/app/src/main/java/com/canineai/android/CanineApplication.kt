package com.canineai.android

import android.app.Application
import com.canineai.android.presentation.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CanineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.attach(this)
    }
}
