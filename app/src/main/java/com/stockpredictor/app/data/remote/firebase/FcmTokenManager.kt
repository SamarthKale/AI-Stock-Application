package com.stockpredictor.app.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Registers/refreshes this device's FCM token under users/{uid}/fcmToken in Firestore, per
 * Phase 2.5's schema. Phase 5c's AlertRuleService reads this to target pushes at this device.
 */
class FcmTokenManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun registerToken(uid: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        }
    }
}
