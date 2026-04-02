package com.stellarflux.ui.screens.skills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stellarflux.data.model.Skill
import com.stellarflux.data.repository.OpenClawRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    openClawRepository: OpenClawRepository,
    onBack: () -> Unit
) {
    val viewModel: SkillsViewModel = viewModel(
        factory = SkillsViewModel.Factory(openClawRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skills") },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.skills.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Extension,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "No skills available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.skills, key = { it.id }) { skill ->
                        SkillCard(
                            skill = skill,
                            onToggle = { viewModel.toggleSkill(skill) },
                            onInvoke = { viewModel.invokeSkill(skill) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    onToggle: () -> Unit,
    onInvoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Icons.Default.Extension,
                contentDescription = null,
                tint = if (skill.enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (skill.description.isNotEmpty()) {
                    Text(
                        text = skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (skill.source.isNotEmpty()) {
                    Text(
                        text = skill.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onInvoke) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Invoke",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Switch(
                checked = skill.enabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
