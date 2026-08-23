package com.stockpredictor.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * Production {@link FirebaseTokenVerifier} — verifies real Firebase ID tokens via the Admin SDK
 * (FirebaseAuth.verifyIdToken), never issuing a separate application JWT (Phase 3 design: Firebase
 * stays the single auth system end-to-end).
 *
 * <p>Credentials are read via Application Default Credentials, i.e. the GOOGLE_APPLICATION_CREDENTIALS
 * environment variable pointing at a downloaded service-account JSON kept outside the repo — never
 * hardcoded and never committed.
 *
 * <p>{@link FirebaseAppInitializer} (Phase 5c — extracted from this class's own former private
 * init method, now shared with the {@code alerts} package's Firestore/FCM-sending code) is
 * lazy on first use rather than at application startup, so the rest of the backend
 * (Postgres/Flyway, the public health endpoint, non-Firebase wiring) still starts up cleanly even
 * in an environment where GOOGLE_APPLICATION_CREDENTIALS isn't set yet — only an actual
 * protected-endpoint call fails, with a clear 401, until credentials are configured.
 */
@Component
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private final FirebaseAppInitializer appInitializer;

    public FirebaseAdminTokenVerifier(FirebaseAppInitializer appInitializer) {
        this.appInitializer = appInitializer;
    }

    @Override
    public FirebaseUserPrincipal verify(String idToken) {
        try {
            appInitializer.ensureInitialized();
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return new FirebaseUserPrincipal(decoded.getUid(), decoded.getEmail(), decoded.getName());
        } catch (FirebaseAuthException e) {
            throw new FirebaseTokenVerificationException("Firebase rejected the token: " + e.getAuthErrorCode(), e);
        } catch (IllegalStateException | IOException e) {
            throw new FirebaseTokenVerificationException(
                    "Firebase Admin SDK is not configured (GOOGLE_APPLICATION_CREDENTIALS missing/invalid)", e);
        }
    }
}
