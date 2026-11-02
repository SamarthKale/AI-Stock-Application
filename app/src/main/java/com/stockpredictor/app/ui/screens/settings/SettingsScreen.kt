package com.stockpredictor.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.ui.components.ClayButton
import com.stockpredictor.app.ui.components.ClayButtonVariant
import com.stockpredictor.app.ui.components.ClayCard
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Profile tab (the old Settings screen; route is still `settings`). v2 changes: a profile header
 * with the photo picker, grouped rows for Wallet / Learn / Offices / Assistant, and the two
 * developer-only sections ("Crash Reporting" and "Preview UI States") removed entirely -- they
 * were never meant for end users. DebugStateController itself is untouched (every ViewModel still
 * reads it); it just stays at NONE now that nothing can change it from the UI.
 */
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToChatbot: () -> Unit = {},
    onNavigateToExchangeMap: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToTutorials: () -> Unit = {},
    onNavigateToDemo: () -> Unit = {},
    onProfilePhotoChanged: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profilePhoto by viewModel.profilePhoto.collectAsStateWithLifecycle()
    val photoUploading by viewModel.photoUploading.collectAsStateWithLifecycle()
    val photoError by viewModel.photoError.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }
    // Keep the drawer/Home avatar in sync once an upload finishes.
    LaunchedEffect(profilePhoto) { if (profilePhoto != null) onProfilePhotoChanged() }

    val launchPicker = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val email = state.userEmail
    val displayName = email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Your profile"

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ClayColor.Background).statusBarsPadding(),
        contentPadding = PaddingValues(start = ClaySpacing.Lg, end = ClaySpacing.Lg, top = ClaySpacing.Lg, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.Lg),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(ClayDimens.AvatarLarge)
                            .clayShadow(shape = CircleShape, elevation = ClayElevation.Default)
                            .clip(CircleShape)
                            .background(ClayColor.Sand)
                            .clickable(onClick = launchPicker),
                        contentAlignment = Alignment.Center,
                    ) {
                        val photo = profilePhoto
                        if (photo != null) {
                            Image(photo.asImageBitmap(), contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = ClayColor.TextSecondary, modifier = Modifier.size(48.dp))
                        }
                        if (photoUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ClayDimens.SpinnerSize * 1.5f),
                                color = ClayColor.AccentPrimary,
                                strokeWidth = ClayDimens.SpinnerStroke,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.size(32.dp).background(ClayColor.AccentGold, CircleShape).clickable(onClick = launchPicker),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Change profile photo", tint = ClayColor.OnDark, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(ClaySpacing.Md))
                Text(displayName, color = ClayColor.TextPrimary, style = MaterialTheme.typography.headlineSmall)
                Text(email ?: "", color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                photoError?.let {
                    Spacer(Modifier.height(ClaySpacing.Xs))
                    Text(it, color = ClayColor.AccentCoral, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SettingsGroup(title = "My Money") {
                SettingsRow(Icons.Filled.AccountBalanceWallet, "My Wallet", "Connect MetaMask and see your real portfolio", onNavigateToWallet)
            }
        }
        item {
            SettingsGroup(title = "Learn") {
                SettingsRow(Icons.Filled.MenuBook, "Tutorials", "Crypto trading and how to use crypto", onNavigateToTutorials)
                RowDivider()
                SettingsRow(Icons.Filled.PlayCircle, "Demo", "A guided tour of this app", onNavigateToDemo)
            }
        }
        item {
            SettingsGroup(title = "Explore") {
                SettingsRow(Icons.Filled.Map, "Our Offices & Map", "Maharashtra offices, directions and exchanges", onNavigateToExchangeMap)
                RowDivider()
                SettingsRow(Icons.Filled.SmartToy, "Ask AI", "Chat with an AI assistant about crypto", onNavigateToChatbot)
            }
        }
        item {
            SettingsGroup(title = "Preferences") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = ClaySpacing.Xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Push notifications", color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::toggleNotifications,
                        colors = SwitchDefaults.colors(checkedTrackColor = ClayColor.AccentPrimary),
                    )
                }
                RowDivider()
                SettingsRow(Icons.Filled.PrivacyTip, "Privacy Policy", null, onNavigateToPrivacyPolicy)
            }
        }
        item {
            SettingsGroup(title = "About") {
                Text("AI Crypto Predictor · v1.0", color = ClayColor.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(ClaySpacing.Xs))
                Text(
                    "Predictions shown in this app are for informational purposes only and " +
                        "are not investment advice.",
                    color = ClayColor.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
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

@Composable
private fun SettingsGroup(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            color = ClayColor.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = ClaySpacing.Sm, bottom = ClaySpacing.Sm),
        )
        ClayCard(modifier = Modifier.fillMaxWidth(), shape = ClayShapes.Large) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(ClayShapes.Small).clickable(onClick = onClick).padding(vertical = ClaySpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(ClayColor.Sand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ClayColor.AccentPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(ClaySpacing.Md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ClayColor.TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ClayColor.TextSecondary)
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(ClayColor.Sand))
}
