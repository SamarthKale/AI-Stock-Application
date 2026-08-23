package com.stockpredictor.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.debug.DebugUiMode
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClaySpacing

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToChatbot: () -> Unit = {},
    onNavigateToExchangeMap: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(title = "Settings")
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(ClaySpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.Lg),
        ) {
            item {
                SettingsSection(title = "Account") {
                    Text("Signed in as demo@user.com", color = ClayColor.TextSecondary)
                }
            }
            item {
                SettingsSection(title = "Notifications") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Push notifications", color = ClayColor.TextPrimary)
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = viewModel::toggleNotifications,
                            colors = SwitchDefaults.colors(checkedTrackColor = ClayColor.AccentPrimary),
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "About") {
                    Text("AI Crypto Predictor · v1.0", color = ClayColor.TextSecondary)
                    Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                    Text(
                        "Predictions shown in this app are for informational purposes only and " +
                            "are not investment advice.",
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                    ClayButton(
                        text = "Privacy Policy",
                        onClick = onNavigateToPrivacyPolicy,
                        variant = ClayButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                SettingsSection(title = "Assistant") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Ask AI", color = ClayColor.TextPrimary)
                            Text("Chat with an AI assistant about crypto", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        ClayButton(text = "Open", onClick = onNavigateToChatbot, variant = ClayButtonVariant.Secondary)
                    }
                }
            }
            item {
                SettingsSection(title = "Markets") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Exchange Map", color = ClayColor.TextPrimary)
                            Text("Major exchanges by live 24h volume", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        ClayButton(text = "Open", onClick = onNavigateToExchangeMap, variant = ClayButtonVariant.Secondary)
                    }
                }
            }
            if (com.stockpredictor.app.BuildConfig.DEBUG) {
                item {
                    SettingsSection(title = "Crash Reporting (debug only)") {
                        Text(
                            "Forces a real crash so it can be verified in the Firebase Crashlytics " +
                                "console. Not shown in release builds.",
                            color = ClayColor.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                        ClayButton(
                            text = "Force Test Crash",
                            onClick = { throw RuntimeException("Phase 6 Crashlytics test crash") },
                            variant = ClayButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Preview UI States") {
                    Text(
                        "Force any data screen into a given state to verify it renders correctly.",
                        color = ClayColor.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(ClaySpacing.Sm))
                    DebugModeRow(selected = state.debugMode, onSelect = viewModel::setDebugMode)
                }
            }
            item {
                ClayButton(
                    text = "Log Out",
                    onClick = viewModel::logout,
                    variant = ClayButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(title, color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(ClaySpacing.Sm))
        ClayCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun DebugModeRow(selected: DebugUiMode, onSelect: (DebugUiMode) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ClaySpacing.Sm),
    ) {
        DebugUiMode.entries.forEach { mode ->
            ClayButton(
                text = debugModeLabel(mode),
                onClick = { onSelect(mode) },
                variant = if (mode == selected) ClayButtonVariant.Primary else ClayButtonVariant.Secondary,
            )
        }
    }
}

private fun debugModeLabel(mode: DebugUiMode): String = when (mode) {
    DebugUiMode.NONE -> "Normal"
    DebugUiMode.LOADING -> "Loading"
    DebugUiMode.EMPTY -> "Empty"
    DebugUiMode.ERROR -> "Error"
}
