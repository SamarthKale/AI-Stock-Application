package com.stockpredictor.app.model

data class NotificationItem(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean,
    val relatedSymbol: String?
)
