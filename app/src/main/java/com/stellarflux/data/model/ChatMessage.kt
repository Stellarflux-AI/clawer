package com.stellarflux.data.model

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val toolUse: ToolUseInfo? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, TOOL
}

data class Attachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val uri: String,
    val size: Long = 0
)

data class ToolUseInfo(
    val toolName: String,
    val status: ToolStatus = ToolStatus.RUNNING,
    val input: String = "",
    val output: String = ""
)

enum class ToolStatus {
    RUNNING, COMPLETED, ERROR
}
