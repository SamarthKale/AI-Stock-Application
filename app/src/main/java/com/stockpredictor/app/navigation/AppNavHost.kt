package com.stockpredictor.app.navigation

import android.app.Application
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
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
import com.stockpredictor.app.ui.screens.cryptodetail.CryptoDetailScreen
import com.stockpredictor.app.ui.screens.watchlist.WatchlistScreen
import com.stockpredictor.app.ui.theme.ClayColor

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current

    // A session persists across cold starts (Firebase Auth's own local storage), so a
    // returning signed-in user skips Onboarding/Login straight to Home. Computed once at
    // first composition — this is a startup routing decision, not a reactive session watch.
    val startDestination = remember {
        if (FirebaseAuthRepository().currentUser != null) Destinations.Home.route else Destinations.Onboarding.route
    }
    LaunchedEffect(Unit) {
        FirebaseAuthRepository().currentUser?.uid?.let { uid ->
            FirestoreSyncRepository.getInstance(context.applicationContext as Application).startListening(uid)
        }
    }

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
            startDestination = startDestination,
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
                    onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) },
                    onSearchClick = { navController.navigate(Destinations.Search.route) },
                    onNotificationsClick = { navController.navigate(Destinations.Notifications.route) },
                )
            }
            composable(Destinations.Watchlist.route) {
                WatchlistScreen(onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) })
            }
            composable(Destinations.Predictions.route) {
                PredictionsScreen(onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) })
            }
            composable(Destinations.Portfolio.route) {
                PortfolioScreen(onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) })
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
                    onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) },
                )
            }
            composable(Destinations.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Destinations.CryptoDetail.route,
                arguments = listOf(navArgument(Destinations.CryptoDetail.ARG_COIN_ID) { type = NavType.StringType }),
            ) { entry ->
                val coinId = entry.arguments?.getString(Destinations.CryptoDetail.ARG_COIN_ID).orEmpty()
                CryptoDetailScreen(coinId = coinId, onBack = { navController.popBackStack() })
            }
        }
    }
}
