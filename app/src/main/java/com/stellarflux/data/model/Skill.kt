package com.stellarflux.data.model

data class Skill(
    val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val source: String = ""
)
