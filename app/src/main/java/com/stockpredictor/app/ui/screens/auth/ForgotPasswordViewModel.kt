package com.stockpredictor.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.toAuthErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordFormState(
    val email: String = "",
    val emailError: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
)

/** Sends a real Firebase Auth password-reset email (Phase 2.5) — the screen composable is unchanged. */
class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = FirebaseAuthRepository()

    private val _formState = MutableStateFlow(ForgotPasswordFormState())
    val formState: StateFlow<ForgotPasswordFormState> = _formState.asStateFlow()

    fun onEmailChange(value: String) {
        _formState.update { it.copy(email = value, emailError = null, isSubmitted = false) }
    }

    fun submit() {
        val current = _formState.value
        if (current.email.isBlank()) {
            _formState.update { it.copy(emailError = "Email is required") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            authRepository.sendPasswordResetEmail(current.email)
                .onSuccess {
                    _formState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                }
                .onFailure { error ->
                    _formState.update {
                        it.copy(isSubmitting = false, emailError = error.toAuthErrorMessage())
                    }
                }
        }
    }
}
