package com.stockpredictor.app.mock

import com.stockpredictor.app.model.NotificationItem

object MockNotifications {
    val all: List<NotificationItem> = listOf(
        NotificationItem(
            id = 1,
            title = "Prediction confidence rising",
            body = "Bitcoin prediction confidence crossed 80%.",
            timestamp = System.currentTimeMillis() - 1_800_000,
            isRead = false,
            relatedSymbol = "bitcoin"
        ),
        NotificationItem(
            id = 2,
            title = "Price alert",
            body = "XRP is up 3.29% today.",
            timestamp = System.currentTimeMillis() - 3_600_000,
            isRead = false,
            relatedSymbol = "ripple"
        ),
        NotificationItem(
            id = 3,
            title = "Watchlist update",
            body = "Dogecoin 30-day prediction is now bullish.",
            timestamp = System.currentTimeMillis() - 7_200_000,
            isRead = true,
            relatedSymbol = "dogecoin"
        ),
        NotificationItem(
            id = 4,
            title = "Weekly summary",
            body = "Your portfolio is up 3.2% this week.",
            timestamp = System.currentTimeMillis() - 86_400_000,
            isRead = true,
            relatedSymbol = null
        ),
        NotificationItem(
            id = 5,
            title = "Price alert",
            body = "Litecoin dropped below your watch threshold.",
            timestamp = System.currentTimeMillis() - 172_800_000,
            isRead = false,
            relatedSymbol = "litecoin"
        ),
        NotificationItem(
            id = 6,
            title = "App update",
            body = "New onboarding tips are available in Settings.",
            timestamp = System.currentTimeMillis() - 259_200_000,
            isRead = true,
            relatedSymbol = null
        )
    )
}
