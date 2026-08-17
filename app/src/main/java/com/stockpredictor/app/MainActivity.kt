package com.stockpredictor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stockpredictor.app.navigation.AppNavHost
import com.stockpredictor.app.ui.theme.ClayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClayTheme {
                AppNavHost()
            }
        }
    }
}
