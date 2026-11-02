package com.stockpredictor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.stockpredictor.app.navigation.Destinations
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayDimens
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.clayShadow
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.ClayTheme

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Destinations.Home.route, "Home", Icons.Filled.Home),
    BottomNavItem(Destinations.Watchlist.route, "Watchlist", Icons.Filled.Star),
    BottomNavItem(Destinations.Predictions.route, "Predictions", Icons.AutoMirrored.Filled.TrendingUp),
    BottomNavItem(Destinations.Portfolio.route, "Portfolio", Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Destinations.Settings.route, "Profile", Icons.Filled.Person),
)

@Composable
fun ClayBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // v2: floating pill with side margins, selected item gets a brown capsule behind it.
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = ClaySpacing.Lg, vertical = ClaySpacing.Sm)
            .fillMaxWidth()
            .height(ClayDimens.NavPillHeight)
            .clayShadow(shape = ClayShapes.Pill, elevation = ClayElevation.Large)
            .background(ClayColor.ClayBase, ClayShapes.Pill)
            .padding(horizontal = ClaySpacing.Sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            val tint = if (selected) ClayColor.OnDark else ClayColor.TextSecondary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(if (selected) ClayColor.AccentPrimary else androidx.compose.ui.graphics.Color.Transparent, ClayShapes.Pill)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onNavigate(item.route) }
                    .padding(vertical = ClaySpacing.Sm, horizontal = ClaySpacing.Md),
            ) {
                Icon(imageVector = item.icon, contentDescription = item.label, tint = tint)
                Text(
                    text = item.label,
                    color = tint,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFEAE4)
@Composable
private fun ClayBottomNavPreview() {
    ClayTheme {
        ClayBottomNav(currentRoute = Destinations.Home.route, onNavigate = {})
    }
}
