package com.stockpredictor.backend.alerts;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.stockpredictor.backend.config.FirebaseAppInitializer;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Production {@link PushSender} — {@link FirebaseMessaging}, already present in the
 * {@code firebase-admin} dependency but unused before Phase 5c (only ID-token verification used
 * this SDK previously).
 */
@Component
public class FirebaseMessagingPushSender implements PushSender {

    private final FirebaseAppInitializer appInitializer;

    public FirebaseMessagingPushSender(FirebaseAppInitializer appInitializer) {
        this.appInitializer = appInitializer;
    }

    @Override
    public void sendDataMessage(String fcmToken, Map<String, String> data) {
        try {
            appInitializer.ensureInitialized();
            Message.Builder builder = Message.builder().setToken(fcmToken);
            data.forEach(builder::putData);
            FirebaseMessaging.getInstance().send(builder.build());
        } catch (FirebaseMessagingException | IOException e) {
            throw new AlertDataUnavailableException("Failed to send FCM push", e);
        }
    }
}
