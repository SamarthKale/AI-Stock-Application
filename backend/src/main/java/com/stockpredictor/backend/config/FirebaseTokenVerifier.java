package com.stockpredictor.backend.config;

/**
 * Verifies a Firebase ID token and returns the identity it encodes. Abstracted behind an
 * interface (rather than calling FirebaseAuth directly from the filter) so tests can substitute
 * a fake verifier instead of requiring a live Firebase project / network call — see
 * FirebaseAdminTokenVerifier for the real Admin SDK-backed production implementation.
 */
public interface FirebaseTokenVerifier {
    FirebaseUserPrincipal verify(String idToken) throws FirebaseTokenVerificationException;
}
