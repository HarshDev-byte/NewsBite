package com.example.newsbite.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    val isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
    
    fun toggleTheme(): Boolean {
        val newValue = !isDarkMode
        prefs.edit().putBoolean(KEY_DARK_MODE, newValue).apply()
        applyTheme(newValue)
        return newValue
    }
    
    fun applyTheme(isDark: Boolean = isDarkMode) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
    
    companion object {
        private const val PREFS_NAME = "newsbite_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
