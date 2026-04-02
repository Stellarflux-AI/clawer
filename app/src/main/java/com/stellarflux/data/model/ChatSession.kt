package com.stellarflux.data.model

data class ChatSession(
    val key: String,
    val label: String,
    val agentId: String = "default",
    val model: String = "",
    val projectId: String? = null,
    val lastMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
