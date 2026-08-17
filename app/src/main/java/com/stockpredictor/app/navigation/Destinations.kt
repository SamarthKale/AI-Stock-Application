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
        /** Only these five show the bottom nav bar (Task 6). */
        val bottomNavRoutes = setOf(
            Home.route, Watchlist.route, Predictions.route, Portfolio.route, Settings.route,
        )
    }
}
