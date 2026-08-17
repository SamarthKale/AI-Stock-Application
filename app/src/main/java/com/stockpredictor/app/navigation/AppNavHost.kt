package com.stockpredictor.app.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockpredictor.app.ui.components.ClayBottomNav
import com.stockpredictor.app.ui.screens.auth.ForgotPasswordScreen
import com.stockpredictor.app.ui.screens.auth.LoginScreen
import com.stockpredictor.app.ui.screens.auth.SignupScreen
import com.stockpredictor.app.ui.screens.home.HomeScreen
import com.stockpredictor.app.ui.screens.notifications.NotificationsScreen
import com.stockpredictor.app.ui.screens.onboarding.OnboardingScreen
import com.stockpredictor.app.ui.screens.portfolio.PortfolioScreen
import com.stockpredictor.app.ui.screens.predictions.PredictionsScreen
import com.stockpredictor.app.ui.screens.search.SearchScreen
import com.stockpredictor.app.ui.screens.settings.SettingsScreen
import com.stockpredictor.app.ui.screens.stockdetail.StockDetailScreen
import com.stockpredictor.app.ui.screens.watchlist.WatchlistScreen
import com.stockpredictor.app.ui.theme.ClayColor

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = ClayColor.Background,
        bottomBar = {
            if (currentRoute in Destinations.bottomNavRoutes) {
                ClayBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Destinations.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        // Back-button behavior: Navigation Compose handles the system back button
        // automatically. Bottom-nav tab switches use popUpTo(Home, saveState=true), so
        // pressing back from any other tab returns to Home; onboarding/login/signup are
        // popped inclusive once the user reaches Home, so Home is the effective root of
        // the back stack — pressing back again there falls through to the Activity's
        // default OnBackPressedDispatcher and exits the app. No custom BackHandler needed.
        NavHost(
            navController = navController,
            startDestination = Destinations.Onboarding.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destinations.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(Destinations.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destinations.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.Onboarding.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate(Destinations.Signup.route) },
                    onNavigateToForgotPassword = { navController.navigate(Destinations.ForgotPassword.route) },
                )
            }
            composable(Destinations.Signup.route) {
                SignupScreen(
                    onSignupSuccess = {
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.Onboarding.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destinations.ForgotPassword.route) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.Home.route) {
                HomeScreen(
                    onStockClick = { symbol -> navController.navigate(Destinations.StockDetail.createRoute(symbol)) },
                    onSearchClick = { navController.navigate(Destinations.Search.route) },
                    onNotificationsClick = { navController.navigate(Destinations.Notifications.route) },
                )
            }
            composable(Destinations.Watchlist.route) {
                WatchlistScreen(onStockClick = { symbol -> navController.navigate(Destinations.StockDetail.createRoute(symbol)) })
            }
            composable(Destinations.Predictions.route) {
                PredictionsScreen(onStockClick = { symbol -> navController.navigate(Destinations.StockDetail.createRoute(symbol)) })
            }
            composable(Destinations.Portfolio.route) {
                PortfolioScreen(onStockClick = { symbol -> navController.navigate(Destinations.StockDetail.createRoute(symbol)) })
            }
            composable(Destinations.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destinations.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onStockClick = { symbol -> navController.navigate(Destinations.StockDetail.createRoute(symbol)) },
                )
            }
            composable(Destinations.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Destinations.StockDetail.route,
                arguments = listOf(navArgument(Destinations.StockDetail.ARG_SYMBOL) { type = NavType.StringType }),
            ) { entry ->
                val symbol = entry.arguments?.getString(Destinations.StockDetail.ARG_SYMBOL).orEmpty()
                StockDetailScreen(symbol = symbol, onBack = { navController.popBackStack() })
            }
        }
    }
}
