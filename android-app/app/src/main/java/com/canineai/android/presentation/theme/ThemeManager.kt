package com.canineai.android.presentation.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "canineai_theme"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_HAS_THEME_PREF = "has_theme_pref"
    private const val KEY_ACTIVE_PROFILE = "active_profile"

    private var prefs: SharedPreferences? = null
    private val _isDarkMode = MutableStateFlow(false)
    private val _hasThemePreference = MutableStateFlow(false)

    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    val hasThemePreference: StateFlow<Boolean> = _hasThemePreference.asStateFlow()

    fun attach(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreProfile(prefs?.getString(KEY_ACTIVE_PROFILE, null))
    }

    /** Selects the theme profile associated with the authenticated account. */
    fun activateProfile(email: String) {
        val profile = email.trim().lowercase().ifBlank { return }
        prefs?.edit()?.putString(KEY_ACTIVE_PROFILE, profile)?.apply()
        restoreProfile(profile)
    }

    fun setDarkMode(value: Boolean) {
        _isDarkMode.value = value
        _hasThemePreference.value = true
        val profile = prefs?.getString(KEY_ACTIVE_PROFILE, null)
        prefs?.edit()
            ?.putBoolean(themeKey(KEY_DARK_MODE, profile), value)
            ?.putBoolean(themeKey(KEY_HAS_THEME_PREF, profile), true)
            ?.apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!(_isDarkMode.value))
    }

    private fun restoreProfile(profile: String?) {
        _hasThemePreference.value = prefs?.getBoolean(themeKey(KEY_HAS_THEME_PREF, profile), false) ?: false
        _isDarkMode.value = prefs?.getBoolean(themeKey(KEY_DARK_MODE, profile), false) ?: false
    }

    private fun themeKey(key: String, profile: String?): String =
        if (profile.isNullOrBlank()) key else "$key.$profile"
}
