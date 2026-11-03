package com.stockpredictor.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.ui.profile.UserProfile
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing

private val DrawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp)

data class DrawerItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Slide-out menu panel (the reference's brown "Hi Vaishali / Manage Users / Devices ..." drawer).
 * Pure content: AppNavHost hosts it inside a ModalNavigationDrawer and closes the drawer around
 * every [DrawerItem.onClick].
 */
@Composable
fun AppDrawerContent(
    profile: UserProfile,
    items: List<DrawerItem>,
    onEditProfile: () -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(ClayDimens.DrawerWidth)
            .fillMaxHeight()
            .background(ClayColor.DrawerSurface, DrawerShape)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ClaySpacing.Xl, vertical = ClaySpacing.Lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close menu", tint = ClayColor.OnDark)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(ClayDimens.AvatarMedium + 8.dp).clip(CircleShape).background(ClayColor.OnDarkMuted),
                contentAlignment = Alignment.Center,
            ) {
                val photo = profile.photo
                if (photo != null) {
                    Image(photo.asImageBitmap(), contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = ClayColor.DrawerSurface)
                }
            }
            Spacer(Modifier.width(ClaySpacing.Md))
            Column {
                Text("Hi ${profile.firstName}", color = ClayColor.OnDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(ClaySpacing.Xs))
                Text(
                    "Edit Profile",
                    color = ClayColor.OnDark,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(ClayShapes.Pill)
                        .clickable(onClick = onEditProfile)
                        .background(ClayColor.OnDark.copy(alpha = 0.18f), ClayShapes.Pill)
                        .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
                )
            }
        }
        Spacer(Modifier.height(ClaySpacing.Lg))
        Box(Modifier.fillMaxWidth().height(1.dp).background(ClayColor.OnDark.copy(alpha = 0.25f)))
        Spacer(Modifier.height(ClaySpacing.Sm))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(ClayShapes.Small).clickable(onClick = item.onClick).padding(vertical = ClaySpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = null, tint = ClayColor.OnDark, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(ClaySpacing.Md))
                    Text(item.label, color = ClayColor.OnDark, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(ClayShapes.Small).clickable(onClick = onLogout).padding(vertical = ClaySpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ClayColor.OnDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(ClaySpacing.Md))
            Text("Logout", color = ClayColor.OnDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
