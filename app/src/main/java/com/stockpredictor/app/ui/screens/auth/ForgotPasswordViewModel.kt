package com.stockpredictor.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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

class ForgotPasswordViewModel : ViewModel() {
    private val _formState = MutableStateFlow(ForgotPasswordFormState())
    val formState: StateFlow<ForgotPasswordFormState> = _formState.asStateFlow()

    fun onEmailChange(value: String) {
        _formState.update { it.copy(email = value, emailError = null, isSubmitted = false) }
    }

    fun submit() {
        val current = _formState.value
        val emailError = validateEmail(current.email)
        if (emailError != null) {
            _formState.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            delay(600)
            _formState.update { it.copy(isSubmitting = false, isSubmitted = true) }
        }
    }
}
