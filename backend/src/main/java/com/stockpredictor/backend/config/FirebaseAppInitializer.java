package com.stockpredictor.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * Shared {@link FirebaseApp} lazy-initialization — extracted from
 * {@link FirebaseAdminTokenVerifier} (Phase 3) so Phase 5c's Firestore/FCM-sending code (the
 * {@code alerts} package) can trigger the same one-time init without duplicating it. Lazy (not
 * {@code @PostConstruct}) for the same reason as before: the rest of the backend (Postgres/Flyway,
 * the public health endpoint) must still start cleanly even in an environment where
 * {@code GOOGLE_APPLICATION_CREDENTIALS} isn't set yet — only an actual Firebase-touching call
 * fails, clearly, until credentials are configured.
 */
@Component
public class FirebaseAppInitializer {

    public synchronized void ensureInitialized() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
