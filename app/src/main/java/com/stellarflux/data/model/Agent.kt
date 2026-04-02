package com.stellarflux.data.model

data class Agent(
    val id: String,
    val name: String,
    val model: String = "",
    val avatar: String? = null,
    val workspace: String? = null,
    val files: List<AgentFile> = emptyList()
)

data class AgentFile(
    val name: String,
    val content: String = "",
    val size: Long = 0,
    val mtime: Long = 0
)
