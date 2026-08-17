package com.stockpredictor.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stockpredictor.app.R
import com.stockpredictor.app.data.remote.firebase.FcmTokenManager
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "stock_alerts"

/**
 * Receive-and-display only in this phase (Phase 2.5) — no in-app Notifications-tab item
 * creation or rule evaluation yet, since that's explicitly Phase 5c's AlertRuleService.
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
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        createChannelIfNeeded()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Stock Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
