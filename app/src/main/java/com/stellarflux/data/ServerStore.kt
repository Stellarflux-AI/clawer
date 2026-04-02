package com.stellarflux.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ServerEntry(
    val id: String,
    val url: String
)

class ServerStore(context: Context) {

    private val prefs = context.getSharedPreferences("servers", Context.MODE_PRIVATE)
    private val KEY = "server_list"

    fun getServers(): List<ServerEntry> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ServerEntry(
                id = obj.getString("id"),
                url = obj.getString("url")
            )
        }
    }

    fun addServer(url: String): ServerEntry {
        val servers = getServers().toMutableList()
        val entry = ServerEntry(
            id = System.currentTimeMillis().toString(),
            url = url
        )
        servers.add(entry)
        save(servers)
        return entry
    }

    fun removeServer(id: String) {
        val servers = getServers().filter { it.id != id }
        save(servers)
    }

    private fun save(servers: List<ServerEntry>) {
        val arr = JSONArray()
        servers.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("url", s.url)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }
}
