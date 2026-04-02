package com.stellarflux.ui.screens.agents

import androidx.compose.foundation.clickable
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
import com.stellarflux.data.model.Agent
import com.stellarflux.data.model.AgentFile
import com.stellarflux.data.repository.OpenClawRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    openClawRepository: OpenClawRepository,
    onBack: () -> Unit
) {
    val viewModel: AgentViewModel = viewModel(
        factory = AgentViewModel.Factory(openClawRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedAgent != null) uiState.selectedAgent!!.name
                        else "Agents"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedAgent != null) viewModel.clearSelection()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.selectedAgent != null) {
                AgentDetail(
                    agent = uiState.selectedAgent!!,
                    files = uiState.agentFiles,
                    editingFile = uiState.editingFile,
                    editingContent = uiState.editingContent,
                    onSelectFile = { viewModel.loadFile(it) },
                    onEditContent = { viewModel.updateEditingContent(it) },
                    onSaveFile = { viewModel.saveFile() },
                    onCancelEdit = { viewModel.cancelEdit() }
                )
            } else {
                AgentList(
                    agents = uiState.agents,
                    onSelectAgent = { viewModel.selectAgent(it) }
                )
            }
        }
    }
}

@Composable
private fun AgentList(
    agents: List<Agent>,
    onSelectAgent: (Agent) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (agents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No agents found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(agents, key = { it.id }) { agent ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectAgent(agent) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = agent.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (agent.model.isNotEmpty()) {
                            Text(
                                text = agent.model,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentDetail(
    agent: Agent,
    files: List<AgentFile>,
    editingFile: String?,
    editingContent: String,
    onSelectFile: (String) -> Unit,
    onEditContent: (String) -> Unit,
    onSaveFile: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Agent info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Agent: ${agent.name}", style = MaterialTheme.typography.titleMedium)
                if (agent.model.isNotEmpty()) {
                    Text("Model: ${agent.model}", style = MaterialTheme.typography.bodySmall)
                }
                if (agent.workspace != null) {
                    Text("Workspace: ${agent.workspace}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Files", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (editingFile != null) {
            // File editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(editingFile, style = MaterialTheme.typography.labelLarge)
                Row {
                    TextButton(onClick = onCancelEdit) { Text("Cancel") }
                    TextButton(onClick = onSaveFile) { Text("Save") }
                }
            }
            OutlinedTextField(
                value = editingContent,
                onValueChange = onEditContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(files, key = { it.name }) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(file.name) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
