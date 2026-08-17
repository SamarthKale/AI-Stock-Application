package com.stockpredictor.app.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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

/**
 * Login is local-only validation in Phase 1 (no real auth yet), so its "ui state" is
 * the form itself rather than the Loading/Empty/Error UiState<T> used by data screens.
 */
class LoginViewModel : ViewModel() {
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
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _formState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            delay(600) // fake network delay so the loading affordance is visually verified
            _formState.update { it.copy(isSubmitting = false) }
            _loginEvent.emit(Unit)
        }
    }
}

internal fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email is required"
    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
    else -> null
}

internal fun validatePassword(password: String): String? = when {
    password.isBlank() -> "Password is required"
    password.length < 6 -> "Password must be at least 6 characters"
    else -> null
}
