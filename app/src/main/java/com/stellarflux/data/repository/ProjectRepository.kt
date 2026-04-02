package com.stellarflux.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stellarflux.data.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.projectDataStore: DataStore<Preferences> by preferencesDataStore(name = "projects")

class ProjectRepository(private val context: Context) {

    private val gson = Gson()
    private val PROJECTS_KEY = stringPreferencesKey("projects_list")

    val projects: Flow<List<Project>> = context.projectDataStore.data.map { prefs ->
        val json = prefs[PROJECTS_KEY] ?: "[]"
        val type = object : TypeToken<List<Project>>() {}.type
        gson.fromJson(json, type)
    }

    suspend fun addProject(project: Project) {
        context.projectDataStore.edit { prefs ->
            val current = getProjectsList(prefs)
            prefs[PROJECTS_KEY] = gson.toJson(current + project)
        }
    }

    suspend fun removeProject(id: String) {
        context.projectDataStore.edit { prefs ->
            val current = getProjectsList(prefs)
            prefs[PROJECTS_KEY] = gson.toJson(current.filter { it.id != id })
        }
    }

    suspend fun updateProject(project: Project) {
        context.projectDataStore.edit { prefs ->
            val current = getProjectsList(prefs)
            prefs[PROJECTS_KEY] = gson.toJson(current.map { if (it.id == project.id) project else it })
        }
    }

    private fun getProjectsList(prefs: Preferences): List<Project> {
        val json = prefs[PROJECTS_KEY] ?: "[]"
        val type = object : TypeToken<List<Project>>() {}.type
        return gson.fromJson(json, type)
    }
}
