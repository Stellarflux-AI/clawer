package com.stellarflux.data.model

data class Server(
    val id: String,
    val name: String,
    val url: String,
    val authToken: String = "",
    val isActive: Boolean = false
)
