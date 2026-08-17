package com.stockpredictor.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { onLoginSuccess() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayColor.Background)
            .padding(ClaySpacing.Lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineSmall, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(ClaySpacing.Xs))
        Text("Log in to continue", color = ClayColor.TextSecondary)
        Spacer(modifier = Modifier.height(ClaySpacing.Xl))
        ClayTextField(
            value = formState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            errorText = formState.emailError,
        )
        Spacer(modifier = Modifier.height(ClaySpacing.Md))
        ClayTextField(
            value = formState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
            errorText = formState.passwordError,
        )
        Spacer(modifier = Modifier.height(ClaySpacing.Sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ClayButton(text = "Forgot password?", onClick = onNavigateToForgotPassword, variant = ClayButtonVariant.Text)
        }
        Spacer(modifier = Modifier.height(ClaySpacing.Md))
        ClayButton(
            text = "Log In",
            onClick = viewModel::submit,
            loading = formState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(ClaySpacing.Lg))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ", color = ClayColor.TextSecondary)
            ClayButton(text = "Sign Up", onClick = onNavigateToSignup, variant = ClayButtonVariant.Text)
        }
    }
}
