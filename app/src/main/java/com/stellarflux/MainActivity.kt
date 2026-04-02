package com.stellarflux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stellarflux.data.model.ChatSession
import com.stellarflux.data.model.Project
import com.stellarflux.data.model.Server
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.ui.navigation.AppNavigation
import com.stellarflux.ui.navigation.Routes
import com.stellarflux.ui.screens.chat.ChatListDrawer
import com.stellarflux.ui.theme.ClawmerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ClawmerApp

        setContent {
            ClawmerTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val servers by app.serverRepository.servers.collectAsState(initial = emptyList())
                val projects by app.projectRepository.projects.collectAsState(initial = emptyList())
                var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
                var currentSessionKey by remember { mutableStateOf<String?>(null) }
                val activeServer = servers.find { it.isActive }
                val connectionState by app.openClawRepository.connectionState.collectAsState()

                // Auto-connect to active server
                LaunchedEffect(activeServer) {
                    if (activeServer != null && connectionState == ConnectionState.DISCONNECTED) {
                        app.openClawRepository.connect(activeServer)
                    }
                }

                // Load sessions when connected
                LaunchedEffect(connectionState) {
                    if (connectionState == ConnectionState.CONNECTED) {
                        try {
                            sessions = app.openClawRepository.listSessions()
                            app.openClawRepository.subscribeSessions()
                        } catch (_: Exception) {}
                    }
                }

                // Listen for session changes
                LaunchedEffect(Unit) {
                    app.openClawRepository.events.collect { event ->
                        if (event is com.stellarflux.data.remote.WsEvent.SessionsChanged) {
                            try {
                                sessions = app.openClawRepository.listSessions()
                            } catch (_: Exception) {}
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ChatListDrawer(
                            sessions = sessions,
                            projects = projects,
                            activeServer = activeServer,
                            currentSessionKey = currentSessionKey,
                            onNewChat = {
                                currentSessionKey = null
                                navController.navigate(Routes.CHAT) {
                                    popUpTo(Routes.CHAT) { inclusive = true }
                                }
                                scope.launch { drawerState.close() }
                            },
                            onSelectSession = { key ->
                                currentSessionKey = key
                                navController.navigate("chat/$key") {
                                    popUpTo(Routes.CHAT) { inclusive = true }
                                }
                                scope.launch { drawerState.close() }
                            },
                            onDeleteSession = { key ->
                                scope.launch {
                                    try {
                                        app.openClawRepository.deleteSession(key)
                                        sessions = sessions.filter { it.key != key }
                                        if (currentSessionKey == key) {
                                            currentSessionKey = null
                                            navController.navigate(Routes.CHAT) {
                                                popUpTo(Routes.CHAT) { inclusive = true }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            onNavigateToServers = {
                                navController.navigate(Routes.SERVERS)
                                scope.launch { drawerState.close() }
                            },
                            onNavigateToSettings = {
                                navController.navigate(Routes.SETTINGS)
                                scope.launch { drawerState.close() }
                            },
                            onCreateProject = { name ->
                                scope.launch {
                                    val serverId = activeServer?.id ?: return@launch
                                    app.projectRepository.addProject(
                                        Project(
                                            id = UUID.randomUUID().toString(),
                                            name = name,
                                            serverId = serverId
                                        )
                                    )
                                }
                            },
                            onAssignToProject = { sessionKey, projectId ->
                                sessions = sessions.map {
                                    if (it.key == sessionKey) it.copy(projectId = projectId) else it
                                }
                            }
                        )
                    }
                ) {
                    AppNavigation(
                        navController = navController,
                        serverRepository = app.serverRepository,
                        projectRepository = app.projectRepository,
                        openClawRepository = app.openClawRepository,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
