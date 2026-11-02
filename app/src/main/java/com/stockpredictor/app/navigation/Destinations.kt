package com.stockpredictor.app.navigation

sealed class Destinations(val route: String) {
    data object Onboarding : Destinations("onboarding")
    data object Login : Destinations("login")
    data object Signup : Destinations("signup")
    data object ForgotPassword : Destinations("forgot_password")
    data object Home : Destinations("home")
    data object Watchlist : Destinations("watchlist")
    data object Predictions : Destinations("predictions")
    data object Portfolio : Destinations("portfolio")
    data object Settings : Destinations("settings")
    data object Search : Destinations("search")
    data object Notifications : Destinations("notifications")
    data object Chatbot : Destinations("chatbot")
    data object ExchangeMap : Destinations("exchange_map")
    data object PrivacyPolicy : Destinations("privacy_policy")

    /** Tutorials/Demo list. [ARG_CATEGORY] is "tutorial", "demo" or "all". */
    data object Learn : Destinations("learn/{category}") {
        const val ARG_CATEGORY = "category"
        fun createRoute(category: String) = "learn/$category"
    }
    data object VideoPlayer : Destinations("video/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun createRoute(itemId: String) = "video/$itemId"
    }
    data object PdfReader : Destinations("pdf/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun createRoute(itemId: String) = "pdf/$itemId"
    }

    data object CryptoDetail : Destinations("crypto_detail/{coinId}") {
        const val ARG_COIN_ID = "coinId"
        fun createRoute(coinId: String) = "crypto_detail/$coinId"
    }

    companion object {
        /**
         * Only these five show the bottom nav bar (Task 6). Deliberately lazy: eagerly
         * referencing sibling nested objects (Home, Watchlist, ...) from this companion's own
         * initializer creates a circular class-init order with the sealed class itself,
         * throwing a NullPointerException the moment anything touches Destinations before the
         * nav graph has "warmed up" every route object in declaration order (e.g. Phase 2.5's
         * AppNavHost reads Destinations.Home.route before building the NavHost at all).
         */
        val bottomNavRoutes: Set<String> by lazy {
            setOf(Home.route, Watchlist.route, Predictions.route, Portfolio.route, Settings.route)
        }

        /**
         * Routes where the global "Ask AI" FAB (root-level placement, see AppNavHost's Scaffold)
         * is hidden: the entire pre-auth flow (nothing to ask about before signing in, and the
         * bubble has no business floating over Login/Signup's own submit buttons) plus the
         * Chatbot screen itself (no point overlaying a shortcut to the screen already open).
         * Every other destination -- Home, Watchlist, Predictions, Portfolio, Settings, Search,
         * Notifications, Crypto Detail, Exchange Map, Privacy Policy -- shows it by default, so a
         * newly added authenticated screen gets the bubble automatically without another edit here.
         */
        val chatbotBubbleHiddenRoutes: Set<String> by lazy {
            setOf(
                Onboarding.route, Login.route, Signup.route, ForgotPassword.route, Chatbot.route,
                // Media screens: the bubble would sit on top of the player's own controls / the page pill.
                VideoPlayer.route, PdfReader.route,
            )
        }
    }
}
