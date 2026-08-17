package com.stockpredictor.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayTextField
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Forgot Password", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().padding(ClaySpacing.Lg)) {
            if (formState.isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = ClaySpacing.Xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = Icons.Filled.MarkEmailRead, contentDescription = null, tint = ClayColor.AccentMint, modifier = Modifier.height(48.dp))
                    Spacer(modifier = Modifier.height(ClaySpacing.Md))
                    Text("Check your email", style = MaterialTheme.typography.titleLarge, color = ClayColor.TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                    Text("We sent a password reset link to ${formState.email}.", color = ClayColor.TextSecondary)
                    Spacer(modifier = Modifier.height(ClaySpacing.Xl))
                    ClayButton(text = "Back to Login", onClick = onBack, variant = ClayButtonVariant.Secondary)
                }
            } else {
                Text(
                    "Enter the email associated with your account and we'll send a reset link.",
                    color = ClayColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xl))
                ClayTextField(
                    value = formState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    errorText = formState.emailError,
                )
                Spacer(modifier = Modifier.height(ClaySpacing.Xl))
                ClayButton(
                    text = "Send Reset Link",
                    onClick = viewModel::submit,
                    loading = formState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
