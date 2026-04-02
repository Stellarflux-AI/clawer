package com.stellarflux.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.stellarflux.data.model.Agent
import com.stellarflux.data.model.AgentFile
import com.stellarflux.data.model.AiModel
import com.stellarflux.data.model.Attachment
import com.stellarflux.data.model.ChatSession
import com.stellarflux.data.model.Server
import com.stellarflux.data.model.Skill
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.data.remote.OpenClawClient
import com.stellarflux.data.remote.WsEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class OpenClawRepository(val client: OpenClawClient = OpenClawClient()) {

    private val gson = Gson()

    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val events: SharedFlow<WsEvent> = client.events

    fun connect(server: Server) = client.connect(server)
    fun disconnect() = client.disconnect()

    suspend fun listSessions(limit: Int = 50): List<ChatSession> {
        val params = JsonObject().apply {
            addProperty("limit", limit)
            addProperty("includeLastMessage", true)
            addProperty("includeDerivedTitles", true)
        }
        val result = client.request("sessions.list", params)
        val sessions = mutableListOf<ChatSession>()
        val items = result.getAsJsonArray("sessions") ?: result.getAsJsonArray("items") ?: JsonArray()
        for (item in items) {
            val obj = item.asJsonObject
            sessions.add(ChatSession(
                key = obj.get("key")?.asString ?: continue,
                label = obj.get("label")?.asString ?: obj.get("title")?.asString ?: "Untitled",
                agentId = obj.get("agentId")?.asString ?: "default",
                model = obj.get("model")?.asString ?: "",
                lastMessage = obj.get("lastMessage")?.asString,
                updatedAt = obj.get("updatedAt")?.asLong ?: System.currentTimeMillis()
            ))
        }
        return sessions
    }

    suspend fun createSession(agentId: String = "default", label: String = "", model: String = ""): ChatSession {
        val params = JsonObject().apply {
            addProperty("agentId", agentId)
            if (label.isNotEmpty()) addProperty("label", label)
            if (model.isNotEmpty()) addProperty("model", model)
        }
        val result = client.request("sessions.create", params)
        return ChatSession(
            key = result.get("key")?.asString ?: result.get("sessionKey")?.asString ?: "",
            label = label.ifEmpty { "New Chat" },
            agentId = agentId,
            model = model
        )
    }

    suspend fun deleteSession(key: String) {
        val params = JsonObject().apply { addProperty("key", key) }
        client.request("sessions.delete", params)
    }

    fun subscribeToSession(key: String) {
        val params = JsonObject().apply { addProperty("key", key) }
        client.send("sessions.messages.subscribe", params)
    }

    fun unsubscribeFromSession(key: String) {
        val params = JsonObject().apply { addProperty("key", key) }
        client.send("sessions.messages.unsubscribe", params)
    }

    suspend fun sendMessage(
        sessionKey: String,
        message: String,
        attachments: List<Attachment> = emptyList()
    ) {
        val params = JsonObject().apply {
            addProperty("key", sessionKey)
            addProperty("message", message)
            if (attachments.isNotEmpty()) {
                val attArr = JsonArray()
                attachments.forEach { att ->
                    attArr.add(JsonObject().apply {
                        addProperty("name", att.name)
                        addProperty("mimeType", att.mimeType)
                        addProperty("uri", att.uri)
                    })
                }
                add("attachments", attArr)
            }
        }
        client.request("sessions.send", params)
    }

    suspend fun abortSession(key: String) {
        val params = JsonObject().apply { addProperty("key", key) }
        client.request("sessions.abort", params)
    }

    suspend fun listAgents(): List<Agent> {
        val result = client.request("agents.list")
        val agents = mutableListOf<Agent>()
        val items = result.getAsJsonArray("agents") ?: result.getAsJsonArray("items") ?: JsonArray()
        for (item in items) {
            val obj = item.asJsonObject
            agents.add(Agent(
                id = obj.get("id")?.asString ?: continue,
                name = obj.get("name")?.asString ?: obj.get("id")?.asString ?: "",
                model = obj.get("model")?.asString ?: "",
                avatar = obj.get("avatar")?.asString,
                workspace = obj.get("workspace")?.asString
            ))
        }
        return agents
    }

    suspend fun getAgentFiles(agentId: String): List<AgentFile> {
        val params = JsonObject().apply { addProperty("agentId", agentId) }
        val result = client.request("agents.files.list", params)
        val files = mutableListOf<AgentFile>()
        val items = result.getAsJsonArray("files") ?: JsonArray()
        for (item in items) {
            val obj = item.asJsonObject
            files.add(AgentFile(
                name = obj.get("name")?.asString ?: continue,
                size = obj.get("size")?.asLong ?: 0,
                mtime = obj.get("mtime")?.asLong ?: 0
            ))
        }
        return files
    }

    suspend fun getAgentFileContent(agentId: String, fileName: String): String {
        val params = JsonObject().apply {
            addProperty("agentId", agentId)
            addProperty("name", fileName)
        }
        val result = client.request("agents.files.get", params)
        return result.get("content")?.asString ?: ""
    }

    suspend fun setAgentFileContent(agentId: String, fileName: String, content: String) {
        val params = JsonObject().apply {
            addProperty("agentId", agentId)
            addProperty("name", fileName)
            addProperty("content", content)
        }
        client.request("agents.files.set", params)
    }

    suspend fun updateAgent(agentId: String, name: String? = null, model: String? = null) {
        val params = JsonObject().apply {
            addProperty("id", agentId)
            if (name != null) addProperty("name", name)
            if (model != null) addProperty("model", model)
        }
        client.request("agents.update", params)
    }

    suspend fun listModels(): List<AiModel> {
        val result = client.request("models.list")
        val models = mutableListOf<AiModel>()
        val items = result.getAsJsonArray("models") ?: result.getAsJsonArray("items") ?: JsonArray()
        for (item in items) {
            val obj = item.asJsonObject
            models.add(AiModel(
                id = obj.get("id")?.asString ?: continue,
                name = obj.get("name")?.asString ?: obj.get("id")?.asString ?: "",
                provider = obj.get("provider")?.asString ?: ""
            ))
        }
        return models
    }

    suspend fun getSkillsStatus(): List<Skill> {
        val result = client.request("skills.status")
        val skills = mutableListOf<Skill>()
        val items = result.getAsJsonArray("skills") ?: result.getAsJsonArray("items") ?: JsonArray()
        for (item in items) {
            val obj = item.asJsonObject
            skills.add(Skill(
                id = obj.get("id")?.asString ?: continue,
                name = obj.get("name")?.asString ?: "",
                description = obj.get("description")?.asString ?: "",
                enabled = obj.get("enabled")?.asBoolean ?: true,
                source = obj.get("source")?.asString ?: ""
            ))
        }
        return skills
    }

    suspend fun updateSkill(skillId: String, enabled: Boolean) {
        val params = JsonObject().apply {
            addProperty("id", skillId)
            addProperty("enabled", enabled)
        }
        client.request("skills.update", params)
    }

    suspend fun getTtsStatus(): JsonObject {
        return client.request("tts.status")
    }

    suspend fun ttsConvert(text: String): JsonObject {
        val params = JsonObject().apply { addProperty("text", text) }
        return client.request("tts.convert", params)
    }

    fun subscribeSessions() {
        client.send("sessions.subscribe")
    }
}
