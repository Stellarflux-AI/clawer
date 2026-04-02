package com.stellarflux.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.stellarflux.data.model.Server
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MockOpenClawClient : OpenClawClient() {

    private val mockSessions = mutableListOf(
        mockSession("session-1", "Welcome Chat", "Hello! How can I help?"),
        mockSession("session-2", "Code Review", "Let me review that code..."),
        mockSession("session-3", "Debug Helper", "I found the issue in line 42")
    )

    private val mockResponses = listOf(
        "I'd be happy to help you with that! Let me think about it for a moment.\n\nHere's what I suggest:\n\n1. First, let's break down the problem into smaller parts\n2. Then we can tackle each one systematically\n3. Finally, we'll verify everything works together\n\nShall I start with the first step?",
        "That's a great question! Based on my analysis, here are the key points:\n\n**Performance**: The current approach has O(n²) complexity. We can optimize it to O(n log n) by using a sorted data structure.\n\n**Readability**: I'd suggest extracting the inner loop into a separate function.\n\nWant me to write the optimized version?",
        "Looking at this more carefully, I think the issue is in how the data flows through the system.\n\nThe WebSocket connection handles the handshake correctly, but the event parsing needs adjustment for the new protocol version.\n\nLet me trace through the code:\n```kotlin\nfun handleEvent(json: JsonObject) {\n    val eventName = json.get(\"event\")?.asString\n    // Process based on event type\n}\n```\n\nThis should fix the streaming issue.",
        "Sure! Here's a quick summary of what we've accomplished:\n\n- Set up the project structure\n- Implemented the core WebSocket client\n- Built the UI with Jetpack Compose\n- Added mock testing support\n\nEverything is looking good! 🚀"
    )

    private var responseIndex = 0

    override fun connect(server: Server) {
        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            delay(300)
            _connectionState.value = ConnectionState.AUTHENTICATING
            delay(400)
            _connectionState.value = ConnectionState.CONNECTED
            _events.emit(WsEvent.Connected)
        }
    }

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun request(method: String, params: JsonObject): JsonObject {
        delay(100) // simulate network
        return when (method) {
            "sessions.list" -> buildSessionsList()
            "sessions.create" -> {
                val key = "session-${UUID.randomUUID().toString().take(8)}"
                val label = params.get("label")?.asString ?: "New Chat"
                mockSessions.add(0, mockSession(key, label, null))
                JsonObject().apply {
                    addProperty("key", key)
                    addProperty("sessionKey", key)
                }
            }
            "sessions.delete" -> {
                val key = params.get("key")?.asString
                mockSessions.removeAll { it.get("key")?.asString == key }
                JsonObject()
            }
            "sessions.send" -> {
                val sessionKey = params.get("key")?.asString ?: ""
                val message = params.get("message")?.asString ?: ""
                // Simulate streaming response in background
                scope.launch { simulateStreaming(sessionKey, message) }
                JsonObject()
            }
            "sessions.abort" -> JsonObject()
            "models.list" -> buildModelsList()
            "agents.list" -> buildAgentsList()
            "agents.files.list" -> buildAgentFilesList()
            "agents.files.get" -> {
                val name = params.get("name")?.asString ?: ""
                JsonObject().apply {
                    addProperty("content", getMockFileContent(name))
                }
            }
            "skills.status" -> buildSkillsList()
            "cron.list" -> buildCronList()
            "cron.remove" -> JsonObject()
            "cron.run" -> JsonObject()
            "sessions.reset" -> JsonObject()
            else -> JsonObject()
        }
    }

    override fun send(method: String, params: JsonObject) {
        // No-op for mock subscriptions
    }

    private suspend fun simulateStreaming(sessionKey: String, userMessage: String) {
        delay(400) // thinking delay

        // Emit a tool use event first
        _events.emit(WsEvent.SessionTool(JsonObject().apply {
            addProperty("tool", "analyze")
            addProperty("name", "analyze")
            addProperty("status", "running")
        }))
        delay(600)
        _events.emit(WsEvent.SessionTool(JsonObject().apply {
            addProperty("tool", "analyze")
            addProperty("name", "analyze")
            addProperty("status", "completed")
        }))
        delay(200)

        // Stream the response word by word
        val response = mockResponses[responseIndex % mockResponses.size]
        responseIndex++

        val words = response.split(" ")
        val chunks = mutableListOf<String>()
        var current = ""
        for (word in words) {
            current += (if (current.isEmpty()) "" else " ") + word
            if (current.length >= 15 || word.endsWith("\n")) {
                chunks.add(current)
                current = ""
            }
        }
        if (current.isNotEmpty()) chunks.add(current)

        for (chunk in chunks) {
            _events.emit(WsEvent.AgentStream(JsonObject().apply {
                addProperty("data", chunk + " ")
                addProperty("runId", "run-mock")
            }))
            delay((30L..80L).random()) // variable typing speed
        }

        // Signal completion
        _events.emit(WsEvent.SessionMessage(JsonObject().apply {
            addProperty("role", "assistant")
            addProperty("done", true)
            addProperty("finished", true)
        }))

        // Update session title based on user message
        val title = if (userMessage.length > 30) userMessage.take(30) + "..." else userMessage
        mockSessions.find { it.get("key")?.asString == sessionKey }?.apply {
            addProperty("label", title)
            addProperty("lastMessage", response.take(60))
        }
        _events.emit(WsEvent.SessionsChanged(JsonObject()))
    }

    private fun buildSessionsList(): JsonObject {
        val arr = JsonArray()
        mockSessions.forEach { arr.add(it) }
        return JsonObject().apply { add("sessions", arr) }
    }

    private fun mockSession(key: String, label: String, lastMessage: String?): JsonObject {
        return JsonObject().apply {
            addProperty("key", key)
            addProperty("label", label)
            addProperty("agentId", "default")
            addProperty("model", "claude-sonnet-4-6")
            if (lastMessage != null) addProperty("lastMessage", lastMessage)
            addProperty("updatedAt", System.currentTimeMillis())
        }
    }

    private fun buildModelsList(): JsonObject {
        val arr = JsonArray()
        listOf(
            Triple("claude-sonnet-4-6", "Claude Sonnet 4.6", "Anthropic"),
            Triple("claude-opus-4-6", "Claude Opus 4.6", "Anthropic"),
            Triple("claude-haiku-4-5", "Claude Haiku 4.5", "Anthropic"),
            Triple("gpt-4o", "GPT-4o", "OpenAI"),
            Triple("gemini-2.5-pro", "Gemini 2.5 Pro", "Google")
        ).forEach { (id, name, provider) ->
            arr.add(JsonObject().apply {
                addProperty("id", id)
                addProperty("name", name)
                addProperty("provider", provider)
            })
        }
        return JsonObject().apply { add("models", arr) }
    }

    private fun buildAgentsList(): JsonObject {
        val arr = JsonArray()
        listOf(
            Triple("default", "Default Agent", "claude-sonnet-4-6"),
            Triple("coder", "Code Assistant", "claude-opus-4-6"),
            Triple("reviewer", "Code Reviewer", "claude-sonnet-4-6")
        ).forEach { (id, name, model) ->
            arr.add(JsonObject().apply {
                addProperty("id", id)
                addProperty("name", name)
                addProperty("model", model)
                addProperty("workspace", "/home/agents/$id")
            })
        }
        return JsonObject().apply { add("agents", arr) }
    }

    private fun buildAgentFilesList(): JsonObject {
        val arr = JsonArray()
        listOf("identity.md", "soul.json", "tools.json", "memory.md").forEach { name ->
            arr.add(JsonObject().apply {
                addProperty("name", name)
                addProperty("size", (100L..2000L).random())
                addProperty("mtime", System.currentTimeMillis())
            })
        }
        return JsonObject().apply { add("files", arr) }
    }

    private fun getMockFileContent(name: String): String = when (name) {
        "identity.md" -> "# Agent Identity\n\nYou are a helpful AI assistant.\n\n## Capabilities\n- Code analysis\n- Bug fixing\n- Code review\n- Documentation"
        "soul.json" -> "{\n  \"personality\": \"helpful\",\n  \"style\": \"concise\",\n  \"expertise\": [\"kotlin\", \"android\", \"compose\"]\n}"
        "tools.json" -> "{\n  \"tools\": [\n    {\"name\": \"read_file\", \"enabled\": true},\n    {\"name\": \"write_file\", \"enabled\": true},\n    {\"name\": \"search\", \"enabled\": true}\n  ]\n}"
        "memory.md" -> "# Agent Memory\n\n- User prefers Kotlin over Java\n- Project uses Jetpack Compose\n- Dark theme preferred"
        else -> "# $name\n\nFile content placeholder."
    }

    private fun buildCronList(): JsonObject {
        val arr = JsonArray()
        listOf(
            mapOf("id" to "cron-1", "name" to "Daily Backup Check", "schedule" to "0 2 * * *", "scheduleType" to "cron", "enabled" to true),
            mapOf("id" to "cron-2", "name" to "Hourly Health Ping", "schedule" to "every 1h", "scheduleType" to "every", "enabled" to true),
            mapOf("id" to "cron-3", "name" to "Weekly Report", "schedule" to "0 9 * * 1", "scheduleType" to "cron", "enabled" to false),
            mapOf("id" to "cron-4", "name" to "Nightly Log Cleanup", "schedule" to "at 03:00", "scheduleType" to "at", "enabled" to true)
        ).forEach { job ->
            arr.add(JsonObject().apply {
                addProperty("id", job["id"] as String)
                addProperty("name", job["name"] as String)
                addProperty("schedule", job["schedule"] as String)
                addProperty("scheduleType", job["scheduleType"] as String)
                addProperty("enabled", job["enabled"] as Boolean)
                addProperty("lastRun", System.currentTimeMillis() - (3600_000L * (1..24).random()))
                addProperty("nextRun", System.currentTimeMillis() + (3600_000L * (1..12).random()))
                addProperty("delivery", "announce")
                addProperty("sessionMode", "isolated")
            })
        }
        return JsonObject().apply { add("jobs", arr) }
    }

    private fun buildSkillsList(): JsonObject {
        val arr = JsonArray()
        listOf(
            Triple("git", "Git Operations", "Commit, push, branch management"),
            Triple("web-search", "Web Search", "Search the web for information"),
            Triple("code-review", "Code Review", "Automated code review and suggestions"),
            Triple("test-runner", "Test Runner", "Run and analyze test suites"),
            Triple("docs-gen", "Doc Generator", "Generate documentation from code")
        ).forEach { (id, name, desc) ->
            arr.add(JsonObject().apply {
                addProperty("id", id)
                addProperty("name", name)
                addProperty("description", desc)
                addProperty("enabled", id != "docs-gen")
                addProperty("source", "builtin")
            })
        }
        return JsonObject().apply { add("skills", arr) }
    }
}
