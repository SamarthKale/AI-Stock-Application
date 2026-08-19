package com.stockpredictor.backend.config;

/** The verified identity extracted from a Firebase ID token. */
public record FirebaseUserPrincipal(String uid, String email, String displayName) {
}
