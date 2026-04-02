package com.stellarflux.data.model

data class CronJob(
    val id: String,
    val name: String,
    val schedule: String,
    val scheduleType: String = "",
    val enabled: Boolean = true,
    val lastRun: Long? = null,
    val nextRun: Long? = null,
    val delivery: String = "none",
    val sessionMode: String = "isolated"
)
