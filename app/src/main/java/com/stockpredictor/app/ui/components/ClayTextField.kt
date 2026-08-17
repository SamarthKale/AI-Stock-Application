package com.stockpredictor.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme
import com.stockpredictor.app.ui.theme.clayShadow

@Composable
fun ClayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
) {
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = errorText != null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = ClayShapes.Small,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ClayColor.ClayBase,
                unfocusedContainerColor = ClayColor.ClayBase,
                disabledContainerColor = ClayColor.ClayBase,
                errorContainerColor = ClayColor.ClayBase,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = ClayColor.AccentCoral,
                focusedLabelColor = ClayColor.AccentPrimary,
                unfocusedLabelColor = ClayColor.TextSecondary,
                errorLabelColor = ClayColor.AccentCoral,
                focusedTextColor = ClayColor.TextPrimary,
                unfocusedTextColor = ClayColor.TextPrimary,
                cursorColor = ClayColor.AccentPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clayShadow(shape = ClayShapes.Small, elevation = ClayElevation.Small),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                color = ClayColor.AccentCoral,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = ClaySpacing.Md, top = ClaySpacing.Xs),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun ClayTextFieldPreview() {
    ClayTheme {
        Column {
            ClayTextField(value = "demo@user.com", onValueChange = {}, label = "Email")
            ClayTextField(value = "", onValueChange = {}, label = "Password", isPassword = true, errorText = "Password is required")
        }
    }
}
