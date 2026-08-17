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

    data object StockDetail : Destinations("stock_detail/{symbol}") {
        const val ARG_SYMBOL = "symbol"
        fun createRoute(symbol: String) = "stock_detail/$symbol"
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
    }
}
