package com.stockpredictor.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around [FirebaseAuth] — sign-up/login/forgot-password screens call this
 * instead of Phase 1's local-only validation. Screen composables are unchanged; only the
 * ViewModels' data source changes (per Phase 2.5's design).
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Reactive session state so [com.stockpredictor.app.navigation.AppNavHost] can redirect
     *  to Login if a session expires, instead of every screen polling [currentUser]. */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Sign up succeeded but no user was returned")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
        user
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Sign in succeeded but no user was returned")
    }

    fun signOut() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }
}

/** Error codes that read as an email-field problem; every other code reads as a password-field problem. */
private val EMAIL_FIELD_ERROR_CODES = setOf(
    "ERROR_INVALID_EMAIL",
    "ERROR_EMAIL_ALREADY_IN_USE",
    "ERROR_USER_NOT_FOUND",
    "ERROR_USER_DISABLED",
)

fun Throwable.isAuthEmailFieldError(): Boolean =
    (this as? FirebaseAuthException)?.errorCode in EMAIL_FIELD_ERROR_CODES

/**
 * Maps Firebase Auth error codes to the same inline [com.stockpredictor.app.ui.components.ClayTextField]
 * error style Phase 1's local validation already used — no raw exception text, no separate error UI.
 */
fun Throwable.toAuthErrorMessage(): String = when ((this as? FirebaseAuthException)?.errorCode) {
    "ERROR_INVALID_EMAIL" -> "Enter a valid email"
    "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters"
    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
    "ERROR_USER_NOT_FOUND" -> "No account found with this email"
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password"
    "ERROR_USER_DISABLED" -> "This account has been disabled"
    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
    "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection"
    else -> "Something went wrong. Please try again."
}
