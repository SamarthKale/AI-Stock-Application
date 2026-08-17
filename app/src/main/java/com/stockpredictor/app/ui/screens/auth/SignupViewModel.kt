package com.stockpredictor.app.ui.screens.auth

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

class SignupViewModel : ViewModel() {
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
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
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
            delay(600)
            _formState.update { it.copy(isSubmitting = false) }
            _signupEvent.emit(Unit)
        }
    }
}
