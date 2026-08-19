package com.stockpredictor.backend.config;

/**
 * Test-only stand-in for {@link FirebaseAdminTokenVerifier} — verifying a real Firebase ID token
 * requires a live Firebase project and a signed-in user, neither of which is available in an
 * automated test run. Recognizes fixed token strings so the auth filter's behavior (accept a
 * valid token, reject an expired/invalid one, reject a missing one) is still exercised end-to-end
 * through real Spring Security + a real Postgres-backed user-provisioning write.
 */
public class FakeFirebaseTokenVerifier implements FirebaseTokenVerifier {

    public static final String VALID_TOKEN = "valid-token";
    public static final String VALID_TOKEN_UID = "test-uid-1";
    public static final String EXPIRED_TOKEN = "expired-token";

    @Override
    public FirebaseUserPrincipal verify(String idToken) {
        if (VALID_TOKEN.equals(idToken)) {
            return new FirebaseUserPrincipal(VALID_TOKEN_UID, "test@example.com", "Test User");
        }
        if (EXPIRED_TOKEN.equals(idToken)) {
            throw new FirebaseTokenVerificationException("Firebase rejected the token: EXPIRED_ID_TOKEN");
        }
        throw new FirebaseTokenVerificationException("Firebase rejected the token: INVALID_ID_TOKEN");
    }
}
