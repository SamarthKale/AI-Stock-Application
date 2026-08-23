package com.stockpredictor.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stockpredictor.app.navigation.AppNavHost
import com.stockpredictor.app.ui.theme.ClayTheme

/**
 * `launchMode="singleTask"` (manifest) + [onNewIntent] here is what makes Phase 5c's alert
 * notification deep links (stockpredictor://crypto_detail/{coinId}) work whether the app is
 * killed (normal onCreate path — Compose's rememberNavController() picks up the launching
 * intent's deep link automatically) or already running (onNewIntent fires instead of a fresh
 * onCreate, so [deepLinkIntent] is what pushes the new intent into [AppNavHost] for
 * NavController.handleDeepLink()).
 */
class MainActivity : ComponentActivity() {

    private var deepLinkIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkIntent = intent
        setContent {
            ClayTheme {
                AppNavHost(deepLinkIntent = deepLinkIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkIntent = intent
    }
}
