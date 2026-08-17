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

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
)

/** Login now authenticates against real Firebase Auth (Phase 2.5) — the screen composable is unchanged. */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = FirebaseAuthRepository()

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _loginEvent = MutableSharedFlow<Unit>()
    val loginEvent: SharedFlow<Unit> = _loginEvent.asSharedFlow()

    fun onEmailChange(value: String) {
        _formState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value, passwordError = null) }
    }

    fun submit() {
        val current = _formState.value
        val emailError = if (current.email.isBlank()) "Email is required" else null
        val passwordError = if (current.password.isBlank()) "Password is required" else null
        if (emailError != null || passwordError != null) {
            _formState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            authRepository.signIn(current.email, current.password)
                .onSuccess { user ->
                    FirestoreSyncRepository.getInstance(getApplication()).startListening(user.uid)
                    // Fire-and-forget: FCM token retrieval can hang on emulators without full
                    // Play Services support, and it's non-critical — must never block navigation.
                    viewModelScope.launch { FcmTokenManager().registerToken(user.uid) }
                    _formState.update { it.copy(isSubmitting = false) }
                    _loginEvent.emit(Unit)
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
