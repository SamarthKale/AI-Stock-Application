package com.stockpredictor.app.ui.screens.auth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupViewModel = viewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op: alerts simply won't display if the user declines */ }

    LaunchedEffect(Unit) {
        viewModel.signupEvent.collect {
            // Requested here — right after first successful auth, with context, not on cold start.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            onSignupSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayColor.Background),
    ) {
        ClayAppBar(title = "Create Account", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ClaySpacing.Lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Join AI Crypto Predictor", style = MaterialTheme.typography.titleLarge, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(ClaySpacing.Xl))
            ClayTextField(value = formState.fullName, onValueChange = viewModel::onFullNameChange, label = "Full name", errorText = formState.fullNameError)
            Spacer(modifier = Modifier.height(ClaySpacing.Md))
            ClayTextField(value = formState.email, onValueChange = viewModel::onEmailChange, label = "Email", keyboardType = KeyboardType.Email, errorText = formState.emailError)
            Spacer(modifier = Modifier.height(ClaySpacing.Md))
            ClayTextField(value = formState.password, onValueChange = viewModel::onPasswordChange, label = "Password", isPassword = true, errorText = formState.passwordError)
            Spacer(modifier = Modifier.height(ClaySpacing.Md))
            ClayTextField(value = formState.confirmPassword, onValueChange = viewModel::onConfirmPasswordChange, label = "Confirm password", isPassword = true, errorText = formState.confirmPasswordError)
            Spacer(modifier = Modifier.height(ClaySpacing.Xl))
            ClayButton(
                text = "Sign Up",
                onClick = viewModel::submit,
                loading = formState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(ClaySpacing.Xl))
        }
    }
}
