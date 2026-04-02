package com.stellarflux.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stellarflux.data.model.ChatSession
import com.stellarflux.data.model.Project
import com.stellarflux.data.model.Server

@Composable
fun ChatListDrawer(
    sessions: List<ChatSession>,
    projects: List<Project>,
    activeServer: Server?,
    currentSessionKey: String?,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCreateProject: (String) -> Unit,
    onAssignToProject: (sessionKey: String, projectId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var sessionToAssign by remember { mutableStateOf<String?>(null) }

    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.height(16.dp))

        // New Chat button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("New Chat")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Sessions grouped by project
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Ungrouped sessions
            val ungrouped = sessions.filter { it.projectId == null }
            if (ungrouped.isNotEmpty()) {
                item {
                    Text(
                        "Chats",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(ungrouped, key = { it.key }) { session ->
                    SessionItem(
                        session = session,
                        isSelected = session.key == currentSessionKey,
                        onClick = { onSelectSession(session.key) },
                        onDelete = { onDeleteSession(session.key) },
                        onAssignProject = { sessionToAssign = session.key }
                    )
                }
            }

            // Project groups
            for (project in projects) {
                val projectSessions = sessions.filter { it.projectId == project.id }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            project.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(projectSessions, key = { it.key }) { session ->
                    SessionItem(
                        session = session,
                        isSelected = session.key == currentSessionKey,
                        onClick = { onSelectSession(session.key) },
                        onDelete = { onDeleteSession(session.key) },
                        onAssignProject = { sessionToAssign = session.key }
                    )
                }
            }

            // Add project button
            item {
                TextButton(
                    onClick = { showNewProjectDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Project", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Server info at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToServers() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeServer?.name ?: "No Server",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = activeServer?.url ?: "Tap to add server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    // New project dialog
    if (showNewProjectDialog) {
        var projectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("New Project") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (projectName.isNotBlank()) {
                            onCreateProject(projectName.trim())
                            showNewProjectDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Assign to project dialog
    sessionToAssign?.let { sessionKey ->
        AlertDialog(
            onDismissRequest = { sessionToAssign = null },
            title = { Text("Move to Project") },
            text = {
                Column {
                    TextButton(onClick = {
                        onAssignToProject(sessionKey, null)
                        sessionToAssign = null
                    }) { Text("No Project") }
                    projects.forEach { project ->
                        TextButton(onClick = {
                            onAssignToProject(sessionKey, project.id)
                            sessionToAssign = null
                        }) { Text(project.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sessionToAssign = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAssignProject: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            session.lastMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Move to Project") },
                    onClick = {
                        showMenu = false
                        onAssignProject()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
