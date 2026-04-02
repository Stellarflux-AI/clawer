package com.stellarflux.ui.screens.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stellarflux.data.model.Server
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.data.repository.OpenClawRepository
import com.stellarflux.data.repository.ServerRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    serverRepository: ServerRepository,
    openClawRepository: OpenClawRepository,
    onBack: () -> Unit
) {
    val viewModel: ServerViewModel = viewModel(
        factory = ServerViewModel.Factory(serverRepository, openClawRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<Server?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Server")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.servers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No servers configured",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Add an OpenClaw server to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(uiState.servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    connectionState = if (server.isActive) uiState.connectionState else null,
                    onActivate = { viewModel.setActiveServer(server.id) },
                    onEdit = { editingServer = server },
                    onDelete = { viewModel.removeServer(server.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        ServerDialog(
            server = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, url, token ->
                viewModel.addServer(name, url, token)
                showAddDialog = false
            }
        )
    }

    editingServer?.let { server ->
        ServerDialog(
            server = server,
            onDismiss = { editingServer = null },
            onSave = { name, url, token ->
                viewModel.updateServer(server.copy(name = name, url = url, authToken = token))
                editingServer = null
            }
        )
    }
}

@Composable
private fun ServerCard(
    server: Server,
    connectionState: ConnectionState?,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (server.isActive) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = if (server.isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = server.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (server.isActive && connectionState != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "Connected"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.AUTHENTICATING -> "Authenticating..."
                            ConnectionState.DISCONNECTED -> "Disconnected"
                            ConnectionState.ERROR -> "Connection Error"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
                            ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (!server.isActive) {
                TextButton(onClick = onActivate) {
                    Text("Connect")
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
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
}

@Composable
private fun ServerDialog(
    server: Server?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, token: String) -> Unit
) {
    var name by remember { mutableStateOf(server?.name ?: "") }
    var url by remember { mutableStateOf(server?.url ?: "") }
    var token by remember { mutableStateOf(server?.authToken ?: "") }
    var showToken by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (server == null) "Add Server" else "Edit Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("192.168.1.100:18789") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Auth Token (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                "Toggle visibility"
                            )
                        }
                    },
                    visualTransformation = if (showToken) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && url.isNotBlank()) onSave(name.trim(), url.trim(), token.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
