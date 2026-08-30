package com.tekphreak.darkbook

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tekphreak.darkbook.data.SettingsStore
import com.tekphreak.darkbook.ui.SplashContent
import com.tekphreak.darkbook.ui.theme.DarkbookTheme

class SplashActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val advanceToMain = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fontFamily = SettingsStore.getFontChoice(this).fontFamily
        setContent {
            DarkbookTheme(fontFamily = fontFamily) {
                SplashContent()
            }
        }
        handler.postDelayed(advanceToMain, 500L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(advanceToMain)
    }
}
