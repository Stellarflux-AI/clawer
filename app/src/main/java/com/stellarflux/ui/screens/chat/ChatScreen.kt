package com.stellarflux.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.data.repository.OpenClawRepository
import com.stellarflux.ui.components.ModelSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionKey: String?,
    openClawRepository: OpenClawRepository,
    onOpenDrawer: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToSkills: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(openClawRepository, sessionKey)
    )
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ModelSelector(
                        currentModel = uiState.currentModel,
                        models = uiState.models,
                        onModelSelected = { viewModel.setModel(it) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    ConnectionBadge(uiState.connectionState)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Agents") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAgents()
                                },
                                leadingIcon = { Icon(Icons.Default.SmartToy, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Skills") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSkills()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendMessage = { text ->
                    if (uiState.sessionKey == null) {
                        viewModel.createNewSession()
                        // Message will be sent after session is created
                        scope.launch {
                            viewModel.uiState.collect { state ->
                                if (state.sessionKey != null && !state.isLoading) {
                                    viewModel.sendMessage(text)
                                    return@collect
                                }
                            }
                        }
                    } else {
                        viewModel.sendMessage(text)
                    }
                },
                onAbort = { viewModel.abortGeneration() },
                isSending = uiState.isSending,
                isRecording = isRecording,
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false },
                onPickFile = { /* TODO: file picker */ },
                onPickImage = { /* TODO: image picker */ },
                onTakePhoto = { /* TODO: camera */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                EmptyChat(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageItem(message = message)
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Auto-clear after showing
            }
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun EmptyChat(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OpenClaw",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionBadge(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
        ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING -> MaterialTheme.colorScheme.primary
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionState.ERROR -> MaterialTheme.colorScheme.error
    }
    val text = when (state) {
        ConnectionState.CONNECTED -> "\u25CF"
        ConnectionState.CONNECTING -> "\u25CC"
        ConnectionState.AUTHENTICATING -> "\u25CE"
        ConnectionState.DISCONNECTED -> "\u25CB"
        ConnectionState.ERROR -> "\u2715"
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}
