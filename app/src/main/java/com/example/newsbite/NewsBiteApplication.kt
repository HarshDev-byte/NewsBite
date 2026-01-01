package com.example.newsbite

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NewsBiteApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme preference
        val prefs = getSharedPreferences("newsbite_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
