package com.stellarflux.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.stellarflux.data.model.*
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.data.remote.WsEvent
import com.stellarflux.data.repository.OpenClawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessionKey: String? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val currentModel: String = "",
    val models: List<AiModel> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null
)

class ChatViewModel(
    private val repository: OpenClawRepository,
    initialSessionKey: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(sessionKey = initialSessionKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingMessageId: String? = null

    init {
        observeConnection()
        observeEvents()
        if (initialSessionKey != null) {
            loadSession(initialSessionKey)
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
                if (state == ConnectionState.CONNECTED) {
                    loadModels()
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is WsEvent.Connected -> {
                        _uiState.value.sessionKey?.let { loadSession(it) }
                    }
                    is WsEvent.AgentStream -> handleAgentStream(event.payload)
                    is WsEvent.SessionMessage -> handleSessionMessage(event.payload)
                    is WsEvent.SessionTool -> handleToolEvent(event.payload)
                    is WsEvent.Error -> {
                        _uiState.value = _uiState.value.copy(error = event.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun createNewSession(agentId: String = "default") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val session = repository.createSession(agentId = agentId, model = _uiState.value.currentModel)
                repository.subscribeToSession(session.key)
                _uiState.value = _uiState.value.copy(
                    sessionKey = session.key,
                    messages = emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadSession(key: String) {
        viewModelScope.launch {
            try {
                repository.subscribeToSession(key)
                _uiState.value = _uiState.value.copy(
                    sessionKey = key,
                    messages = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun sendMessage(text: String, attachments: List<Attachment> = emptyList()) {
        val sessionKey = _uiState.value.sessionKey ?: return
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
            attachments = attachments
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isSending = true
        )

        viewModelScope.launch {
            try {
                repository.sendMessage(sessionKey, text, attachments)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message
                )
            }
        }
    }

    fun abortGeneration() {
        val sessionKey = _uiState.value.sessionKey ?: return
        viewModelScope.launch {
            try {
                repository.abortSession(sessionKey)
                finishStreaming()
            } catch (_: Exception) {}
        }
    }

    fun setModel(model: AiModel) {
        _uiState.value = _uiState.value.copy(currentModel = model.id)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val models = repository.listModels()
                _uiState.value = _uiState.value.copy(
                    models = models,
                    currentModel = _uiState.value.currentModel.ifEmpty {
                        models.firstOrNull()?.id ?: ""
                    }
                )
            } catch (_: Exception) {}
        }
    }

    private fun handleAgentStream(payload: JsonObject) {
        val data = payload.get("data")?.asString
            ?: payload.get("content")?.asString
            ?: payload.get("text")?.asString
            ?: return

        val messages = _uiState.value.messages.toMutableList()
        val existingIdx = messages.indexOfLast { it.id == streamingMessageId }

        if (existingIdx >= 0) {
            val existing = messages[existingIdx]
            messages[existingIdx] = existing.copy(
                content = existing.content + data,
                isStreaming = true
            )
        } else {
            val newId = UUID.randomUUID().toString()
            streamingMessageId = newId
            messages.add(ChatMessage(
                id = newId,
                role = MessageRole.ASSISTANT,
                content = data,
                isStreaming = true
            ))
        }
        _uiState.value = _uiState.value.copy(messages = messages, isSending = false)
    }

    private fun handleSessionMessage(payload: JsonObject) {
        val role = payload.get("role")?.asString
        val content = payload.get("content")?.asString
            ?: payload.get("text")?.asString
            ?: payload.get("message")?.asString

        if (role == "assistant" && content != null) {
            val messages = _uiState.value.messages.toMutableList()
            val existingIdx = messages.indexOfLast { it.id == streamingMessageId }
            if (existingIdx >= 0) {
                messages[existingIdx] = messages[existingIdx].copy(
                    content = content,
                    isStreaming = false
                )
            } else {
                messages.add(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = content
                ))
            }
            finishStreaming()
            _uiState.value = _uiState.value.copy(messages = messages, isSending = false)
        }

        val done = payload.get("done")?.asBoolean ?: false
        val finished = payload.get("finished")?.asBoolean ?: false
        if (done || finished) {
            finishStreaming()
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    private fun handleToolEvent(payload: JsonObject) {
        val toolName = payload.get("tool")?.asString
            ?: payload.get("name")?.asString ?: "tool"
        val status = payload.get("status")?.asString ?: "running"
        val toolStatus = when (status) {
            "completed", "done", "success" -> ToolStatus.COMPLETED
            "error", "failed" -> ToolStatus.ERROR
            else -> ToolStatus.RUNNING
        }

        val messages = _uiState.value.messages.toMutableList()
        messages.add(ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.TOOL,
            content = toolName,
            toolUse = ToolUseInfo(
                toolName = toolName,
                status = toolStatus,
                input = payload.get("input")?.toString() ?: "",
                output = payload.get("output")?.toString() ?: ""
            )
        ))
        _uiState.value = _uiState.value.copy(messages = messages)
    }

    private fun finishStreaming() {
        streamingMessageId?.let { id ->
            val messages = _uiState.value.messages.toMutableList()
            val idx = messages.indexOfLast { it.id == id }
            if (idx >= 0) {
                messages[idx] = messages[idx].copy(isStreaming = false)
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        streamingMessageId = null
    }

    class Factory(
        private val repository: OpenClawRepository,
        private val sessionKey: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(repository, sessionKey) as T
        }
    }
}
