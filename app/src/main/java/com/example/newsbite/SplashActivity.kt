package com.example.newsbite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private var keepSplashScreen = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        // Navigate after splash exits
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
        
        // Simulate initialization (can be replaced with actual init logic)
        lifecycleScope.launch {
            delay(1500) // Brief delay for branding
            keepSplashScreen = false
        }
    }
}
