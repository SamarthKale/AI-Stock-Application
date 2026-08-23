package com.stockpredictor.app.data.local.entity

enum class ChatRole { USER, ASSISTANT }

data class ChatMessageEntity(
    val id: Long,
    val conversationId: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
)
