package com.stellarflux.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stellarflux.data.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "servers")

class ServerRepository(private val context: Context) {

    private val gson = Gson()
    private val SERVERS_KEY = stringPreferencesKey("servers_list")

    val servers: Flow<List<Server>> = context.dataStore.data.map { prefs ->
        val json = prefs[SERVERS_KEY] ?: "[]"
        val type = object : TypeToken<List<Server>>() {}.type
        gson.fromJson(json, type)
    }

    suspend fun addServer(server: Server) {
        context.dataStore.edit { prefs ->
            val current = getServersList(prefs)
            val updated = current + server
            prefs[SERVERS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun updateServer(server: Server) {
        context.dataStore.edit { prefs ->
            val current = getServersList(prefs)
            val updated = current.map { if (it.id == server.id) server else it }
            prefs[SERVERS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun removeServer(id: String) {
        context.dataStore.edit { prefs ->
            val current = getServersList(prefs)
            val updated = current.filter { it.id != id }
            prefs[SERVERS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun setActiveServer(id: String) {
        context.dataStore.edit { prefs ->
            val current = getServersList(prefs)
            val updated = current.map { it.copy(isActive = it.id == id) }
            prefs[SERVERS_KEY] = gson.toJson(updated)
        }
    }

    private fun getServersList(prefs: Preferences): List<Server> {
        val json = prefs[SERVERS_KEY] ?: "[]"
        val type = object : TypeToken<List<Server>>() {}.type
        return gson.fromJson(json, type)
    }
}
