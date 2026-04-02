package com.stellarflux.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stellarflux.data.model.Server
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OpenClawClient {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentServer: Server? = null
    private var deviceToken: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonObject>>()
    private var challengeNonce: String? = null

    fun connect(server: Server) {
        disconnect()
        currentServer = server
        _connectionState.value = ConnectionState.CONNECTING

        val wsUrl = server.url.let { url ->
            when {
                url.startsWith("ws://") || url.startsWith("wss://") -> url
                url.startsWith("http://") -> url.replace("http://", "ws://")
                url.startsWith("https://") -> url.replace("https://", "wss://")
                else -> "ws://$url"
            }
        }

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.AUTHENTICATING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.ERROR
                scope.launch { _events.emit(WsEvent.Error(t.message ?: "Connection failed")) }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()
    }

    suspend fun request(method: String, params: JsonObject = JsonObject()): JsonObject {
        val id = UUID.randomUUID().toString()
        val frame = JsonObject().apply {
            addProperty("type", "req")
            addProperty("id", id)
            addProperty("method", method)
            add("params", params)
        }
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        webSocket?.send(gson.toJson(frame))
        return deferred.await()
    }

    fun send(method: String, params: JsonObject = JsonObject()) {
        val id = UUID.randomUUID().toString()
        val frame = JsonObject().apply {
            addProperty("type", "req")
            addProperty("id", id)
            addProperty("method", method)
            add("params", params)
        }
        webSocket?.send(gson.toJson(frame))
    }

    private fun handleMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val type = json.get("type")?.asString ?: return

            when (type) {
                "event" -> handleEvent(json)
                "res" -> handleResponse(json)
            }
        } catch (e: Exception) {
            scope.launch { _events.emit(WsEvent.Error("Parse error: ${e.message}")) }
        }
    }

    private fun handleEvent(json: JsonObject) {
        val eventName = json.get("event")?.asString ?: return
        val payload = json.getAsJsonObject("payload") ?: JsonObject()

        when (eventName) {
            "connect.challenge" -> {
                challengeNonce = payload.get("nonce")?.asString
                sendConnectRequest()
            }
            "agent" -> scope.launch {
                _events.emit(WsEvent.AgentStream(payload))
            }
            "session.message" -> scope.launch {
                _events.emit(WsEvent.SessionMessage(payload))
            }
            "session.tool" -> scope.launch {
                _events.emit(WsEvent.SessionTool(payload))
            }
            "sessions.changed" -> scope.launch {
                _events.emit(WsEvent.SessionsChanged(payload))
            }
            "tick" -> { /* heartbeat, ignore */ }
            else -> scope.launch {
                _events.emit(WsEvent.Generic(eventName, payload))
            }
        }
    }

    private fun handleResponse(json: JsonObject) {
        val id = json.get("id")?.asString ?: return
        val ok = json.get("ok")?.asBoolean ?: false
        val payload = json.getAsJsonObject("payload") ?: json.getAsJsonObject("error") ?: JsonObject()

        if (ok && payload.get("type")?.asString == "hello-ok") {
            val auth = payload.getAsJsonObject("auth")
            deviceToken = auth?.get("deviceToken")?.asString
            _connectionState.value = ConnectionState.CONNECTED
            scope.launch { _events.emit(WsEvent.Connected) }
        }

        pendingRequests.remove(id)?.complete(
            if (ok) payload else JsonObject().apply {
                addProperty("error", true)
                add("details", payload)
            }
        )
    }

    private fun sendConnectRequest() {
        val server = currentServer ?: return
        val params = JsonObject().apply {
            addProperty("minProtocol", 3)
            addProperty("maxProtocol", 3)
            add("client", JsonObject().apply {
                addProperty("id", "clawmer-android")
                addProperty("version", "1.0.0")
                addProperty("platform", "android")
                addProperty("mode", "operator")
                addProperty("displayName", "Clawmer Android")
                addProperty("deviceFamily", "android")
            })
            addProperty("role", "operator")
            add("scopes", gson.toJsonTree(listOf("operator.read", "operator.write")))
            add("caps", gson.toJsonTree(listOf("camera", "voice")))
            add("auth", JsonObject().apply {
                addProperty("token", server.authToken)
                if (deviceToken != null) addProperty("deviceToken", deviceToken)
            })
            addProperty("userAgent", "clawmer-android/1.0.0")
            if (challengeNonce != null) {
                add("device", JsonObject().apply {
                    addProperty("id", "clawmer-${server.id}")
                    addProperty("nonce", challengeNonce)
                    addProperty("signedAt", System.currentTimeMillis())
                })
            }
        }

        val id = UUID.randomUUID().toString()
        val frame = JsonObject().apply {
            addProperty("type", "req")
            addProperty("id", id)
            addProperty("method", "connect")
            add("params", params)
        }
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        webSocket?.send(gson.toJson(frame))
    }
}

sealed class WsEvent {
    data object Connected : WsEvent()
    data class Error(val message: String) : WsEvent()
    data class AgentStream(val payload: JsonObject) : WsEvent()
    data class SessionMessage(val payload: JsonObject) : WsEvent()
    data class SessionTool(val payload: JsonObject) : WsEvent()
    data class SessionsChanged(val payload: JsonObject) : WsEvent()
    data class Generic(val event: String, val payload: JsonObject) : WsEvent()
}
