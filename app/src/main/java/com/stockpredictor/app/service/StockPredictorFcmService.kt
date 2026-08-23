package com.stockpredictor.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stockpredictor.app.MainActivity
import com.stockpredictor.app.R
import com.stockpredictor.app.data.local.dao.NotificationDao
import com.stockpredictor.app.data.remote.firebase.FcmTokenManager
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "crypto_alerts"

/**
 * Phase 5c: the backend's AlertRuleService sends a DATA-ONLY message (see its Javadoc for why --
 * no `notification` block), so
 * [onMessageReceived] fires uniformly whether the app is foregrounded, backgrounded, or (per
 * standard FCM behavior) recently killed. Every received alert push is handled the same way,
 * regardless of process state: persisted as a real [com.stockpredictor.app.model.NotificationItem]
 * row (so the Notifications tab has real history, not mock data) and shown as a system
 * notification whose tap deep-links to the coin's detail screen via a plain `ACTION_VIEW` Intent
 * (see AndroidManifest.xml's `stockpredictor://` intent-filter and AppNavHost's `navDeepLink`).
 */
class StockPredictorFcmService : FirebaseMessagingService() {

    /** Tokens can rotate — re-register whenever that happens, not just on first login. */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuthRepository().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            FcmTokenManager().registerToken(uid)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val coinId = message.data["coinId"]
        val headline = message.data["headline"] ?: message.notification?.body ?: message.data["body"]
        val title = titleFor(message.data["ruleType"]) ?: message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        if (headline == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val timestamp = System.currentTimeMillis()
            NotificationDao(applicationContext).insert(title = title, body = headline, timestamp = timestamp, relatedCoinId = coinId)
            showNotification(title, headline, coinId)
        }
    }

    private fun titleFor(ruleType: String?): String? = when (ruleType) {
        "PRICE_MOVE" -> "Price Alert"
        "PREDICTION_CONFIDENCE" -> "Prediction Alert"
        else -> null
    }

    private fun showNotification(title: String, body: String, coinId: String?) {
        createChannelIfNeeded()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
        if (coinId != null) {
            builder.setContentIntent(deepLinkPendingIntent(coinId))
        }
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    /** Plain ACTION_VIEW Intent + the manifest's `stockpredictor://` intent-filter + AppNavHost's
     *  navDeepLink registration on CryptoDetail -- the standard, documented way to deep-link into
     *  a Compose Navigation destination without Fragment-Navigation's NavDeepLinkBuilder, and it
     *  works uniformly whether the app is foregrounded, backgrounded, or killed at tap time. */
    private fun deepLinkPendingIntent(coinId: String): PendingIntent {
        val uri = Uri.parse("stockpredictor://crypto_detail/$coinId")
        val intent = Intent(Intent.ACTION_VIEW, uri, this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            coinId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Crypto Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
