package com.stockpredictor.app.navigation

import android.app.Application
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpredictor.app.data.learn.LearnCategory
import com.stockpredictor.app.data.learn.LearnType
import com.stockpredictor.app.ui.components.AppDrawerContent
import com.stockpredictor.app.ui.screens.learn.LearnScreen
import com.stockpredictor.app.ui.screens.learn.PdfReaderScreen
import com.stockpredictor.app.ui.screens.learn.VideoPlayerScreen
import com.stockpredictor.app.ui.components.DrawerItem
import com.stockpredictor.app.ui.profile.UserProfileViewModel
import kotlinx.coroutines.launch
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
import androidx.navigation.navDeepLink
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.ui.components.ChatbotFab
import com.stockpredictor.app.ui.components.ClayBottomNav
import com.stockpredictor.app.ui.screens.auth.ForgotPasswordScreen
import com.stockpredictor.app.ui.screens.auth.LoginScreen
import com.stockpredictor.app.ui.screens.auth.SignupScreen
import com.stockpredictor.app.ui.screens.chatbot.ChatbotScreen
import com.stockpredictor.app.ui.screens.exchangemap.ExchangeMapScreen
import com.stockpredictor.app.ui.screens.home.HomeScreen
import com.stockpredictor.app.ui.screens.notifications.NotificationsScreen
import com.stockpredictor.app.ui.screens.onboarding.OnboardingScreen
import com.stockpredictor.app.ui.screens.portfolio.PortfolioScreen
import com.stockpredictor.app.ui.screens.predictions.PredictionsScreen
import com.stockpredictor.app.ui.screens.search.SearchScreen
import com.stockpredictor.app.ui.screens.settings.PrivacyPolicyScreen
import com.stockpredictor.app.ui.screens.settings.SettingsScreen
import com.stockpredictor.app.ui.screens.cryptodetail.CryptoDetailScreen
import com.stockpredictor.app.ui.screens.watchlist.WatchlistScreen
import com.stockpredictor.app.ui.theme.ClayColor

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    deepLinkIntent: Intent? = null,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current

    // Phase 5c: pushes an alert-notification deep link (stockpredictor://crypto_detail/{coinId})
    // into the graph — covers both cold start (MainActivity.onCreate) and already-running
    // (onNewIntent) cases uniformly, since MainActivity always routes through this same state.
    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.let { navController.handleDeepLink(it) }
    }

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

    // v2 drawer: one activity-scoped profile (name/photo) shared by the Home greeting and the menu.
    val profileViewModel: UserProfileViewModel = viewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = {
        profileViewModel.refreshPhoto()
        scope.launch { drawerState.open() }
    }
    // Navigates from a drawer row: close first, then go; top-level tabs use the same
    // single-top/restore-state options as the bottom nav so back-stack behaviour is identical.
    fun drawerGo(route: String, topLevel: Boolean = false) {
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            launchSingleTop = true
            if (topLevel) {
                popUpTo(Destinations.Home.route) { saveState = true }
                restoreState = true
            }
        }
    }
    LaunchedEffect(Unit) {
        profileViewModel.logoutEvent.collect {
            drawerState.close()
            navController.navigate(Destinations.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe-to-open only on the five tab roots; never over auth screens, map gestures or players.
        gesturesEnabled = drawerState.isOpen || currentRoute in Destinations.bottomNavRoutes,
        drawerContent = {
            AppDrawerContent(
                profile = profile,
                items = listOf(
                    DrawerItem("Home", Icons.Filled.Home) { drawerGo(Destinations.Home.route, topLevel = true) },
                    DrawerItem("My Wallet", Icons.Filled.AccountBalanceWallet) { drawerGo(Destinations.Portfolio.route, topLevel = true) },
                    DrawerItem("Tutorials & Demo", Icons.Filled.MenuBook) { drawerGo(Destinations.Learn.createRoute("all")) },
                    DrawerItem("Our Offices", Icons.Filled.Map) { drawerGo(Destinations.ExchangeMap.route) },
                    DrawerItem("Ask AI", Icons.Filled.SmartToy) { drawerGo(Destinations.Chatbot.route) },
                    DrawerItem("Notifications", Icons.Filled.Notifications) { drawerGo(Destinations.Notifications.route) },
                    DrawerItem("Privacy Policy", Icons.Filled.PrivacyTip) { drawerGo(Destinations.PrivacyPolicy.route) },
                ),
                onEditProfile = { drawerGo(Destinations.Settings.route, topLevel = true) },
                onClose = { scope.launch { drawerState.close() } },
                onLogout = profileViewModel::logout,
            )
        },
    ) {
    Scaffold(
        containerColor = ClayColor.Background,
        // Global "Ask AI" bubble: one root-level placement instead of duplicating it per screen
        // (see Destinations.chatbotBubbleHiddenRoutes for exactly which routes hide it). Scaffold
        // positions this slot bottom-end with its own standard spacing and already accounts for
        // bottomBar's height, so the bubble never overlaps the bottom nav on the 5 top-level tabs.
        floatingActionButton = {
            if (currentRoute != null && currentRoute !in Destinations.chatbotBubbleHiddenRoutes) {
                ChatbotFab(onClick = {
                    navController.navigate(Destinations.Chatbot.route) { launchSingleTop = true }
                })
            }
        },
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
                            // popUpTo(0), not Onboarding: Login can be reached either from
                            // Onboarding (first run) or straight from a post-Logout back stack
                            // that already popped Onboarding away entirely (see Logout below) —
                            // popping to the graph root unconditionally is what actually
                            // guarantees Login never survives underneath Home either way.
                            popUpTo(0) { inclusive = true }
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
                            // Same popUpTo(0) reasoning as Login's onLoginSuccess above.
                            popUpTo(0) { inclusive = true }
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
                    profile = profile,
                    onMenuClick = openDrawer,
                    onOfficesClick = { navController.navigate(Destinations.ExchangeMap.route) },
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
                    onNavigateToChatbot = { navController.navigate(Destinations.Chatbot.route) },
                    onNavigateToExchangeMap = { navController.navigate(Destinations.ExchangeMap.route) },
                    onNavigateToPrivacyPolicy = { navController.navigate(Destinations.PrivacyPolicy.route) },
                    onNavigateToWallet = {
                        navController.navigate(Destinations.Portfolio.route) {
                            popUpTo(Destinations.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTutorials = { navController.navigate(Destinations.Learn.createRoute("tutorial")) },
                    onNavigateToDemo = { navController.navigate(Destinations.Learn.createRoute("demo")) },
                    onProfilePhotoChanged = profileViewModel::refreshPhoto,
                )
            }
            composable(Destinations.Chatbot.route) {
                ChatbotScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.ExchangeMap.route) {
                ExchangeMapScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Destinations.Learn.route,
                arguments = listOf(navArgument(Destinations.Learn.ARG_CATEGORY) { type = NavType.StringType }),
            ) { entry ->
                val category = when (entry.arguments?.getString(Destinations.Learn.ARG_CATEGORY)) {
                    "tutorial" -> LearnCategory.TUTORIAL
                    "demo" -> LearnCategory.DEMO
                    else -> null
                }
                LearnScreen(
                    category = category,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { item ->
                        navController.navigate(
                            if (item.type == LearnType.VIDEO) Destinations.VideoPlayer.createRoute(item.id)
                            else Destinations.PdfReader.createRoute(item.id)
                        )
                    },
                )
            }
            composable(
                route = Destinations.VideoPlayer.route,
                arguments = listOf(navArgument(Destinations.VideoPlayer.ARG_ITEM_ID) { type = NavType.StringType }),
            ) { entry ->
                VideoPlayerScreen(
                    itemId = entry.arguments?.getString(Destinations.VideoPlayer.ARG_ITEM_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destinations.PdfReader.route,
                arguments = listOf(navArgument(Destinations.PdfReader.ARG_ITEM_ID) { type = NavType.StringType }),
            ) { entry ->
                PdfReaderScreen(
                    itemId = entry.arguments?.getString(Destinations.PdfReader.ARG_ITEM_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destinations.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) },
                )
            }
            composable(Destinations.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onCoinClick = { coinId -> navController.navigate(Destinations.CryptoDetail.createRoute(coinId)) },
                )
            }
            composable(
                route = Destinations.CryptoDetail.route,
                arguments = listOf(navArgument(Destinations.CryptoDetail.ARG_COIN_ID) { type = NavType.StringType }),
                // Phase 5c: lets an alert-notification PendingIntent (built by
                // StockPredictorFcmService as a plain ACTION_VIEW Intent) land directly on this
                // screen — see MainActivity/AppNavHost's deepLinkIntent handling above.
                deepLinks = listOf(navDeepLink { uriPattern = "stockpredictor://crypto_detail/{coinId}" }),
            ) { entry ->
                val coinId = entry.arguments?.getString(Destinations.CryptoDetail.ARG_COIN_ID).orEmpty()
                CryptoDetailScreen(coinId = coinId, onBack = { navController.popBackStack() })
            }
        }
    }
    } // ModalNavigationDrawer
}
