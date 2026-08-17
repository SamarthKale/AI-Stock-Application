package com.stockpredictor.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.remote.firebase.FcmTokenManager
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.data.remote.firebase.isAuthEmailFieldError
import com.stockpredictor.app.data.remote.firebase.toAuthErrorMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignupFormState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
)

/** Sign-up now creates a real Firebase Auth account (Phase 2.5) — the screen composable is unchanged. */
class SignupViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = FirebaseAuthRepository()

    private val _formState = MutableStateFlow(SignupFormState())
    val formState: StateFlow<SignupFormState> = _formState.asStateFlow()

    private val _signupEvent = MutableSharedFlow<Unit>()
    val signupEvent: SharedFlow<Unit> = _signupEvent.asSharedFlow()

    fun onFullNameChange(value: String) = _formState.update { it.copy(fullName = value, fullNameError = null) }
    fun onEmailChange(value: String) = _formState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _formState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null) }
    fun onConfirmPasswordChange(value: String) = _formState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    fun submit() {
        val current = _formState.value
        val fullNameError = if (current.fullName.isBlank()) "Name is required" else null
        val emailError = if (current.email.isBlank()) "Email is required" else null
        val passwordError = if (current.password.isBlank()) "Password is required" else null
        val confirmPasswordError = when {
            current.confirmPassword.isBlank() -> "Confirm your password"
            current.confirmPassword != current.password -> "Passwords don't match"
            else -> null
        }
        if (listOfNotNull(fullNameError, emailError, passwordError, confirmPasswordError).isNotEmpty()) {
            _formState.update {
                it.copy(
                    fullNameError = fullNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            authRepository.signUp(current.email, current.password, current.fullName)
                .onSuccess { user ->
                    FirestoreSyncRepository.getInstance(getApplication()).startListening(user.uid)
                    // Fire-and-forget: FCM token retrieval can hang on emulators without full
                    // Play Services support, and it's non-critical — must never block navigation.
                    viewModelScope.launch { FcmTokenManager().registerToken(user.uid) }
                    _formState.update { it.copy(isSubmitting = false) }
                    _signupEvent.emit(Unit)
                }
                .onFailure { error ->
                    val message = error.toAuthErrorMessage()
                    val isEmailError = error.isAuthEmailFieldError()
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = if (isEmailError) message else null,
                            passwordError = if (!isEmailError) message else null,
                        )
                    }
                }
        }
    }
}
