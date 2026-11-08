package org.std81atmosphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.std81atmosphere.ui.App
import org.std81atmosphere.ui.Std81Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val language = preferences.getString("language", "ru") ?: "ru"
        setContent {
            Std81Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(initialLanguage = language)
                }
            }
        }
    }
}