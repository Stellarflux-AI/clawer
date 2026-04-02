package com.stellarflux.data.remote

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    ERROR
}
