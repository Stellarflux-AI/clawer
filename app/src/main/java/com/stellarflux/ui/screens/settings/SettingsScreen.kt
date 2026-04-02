package com.stellarflux.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToSkills: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Connection") {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "Servers",
                    subtitle = "Manage OpenClaw servers",
                    onClick = onNavigateToServers
                )
            }

            SettingsSection("AI") {
                SettingsItem(
                    icon = Icons.Default.SmartToy,
                    title = "Agents",
                    subtitle = "View and edit agents",
                    onClick = onNavigateToAgents
                )
                SettingsItem(
                    icon = Icons.Default.Extension,
                    title = "Skills",
                    subtitle = "Manage skills and tools",
                    onClick = onNavigateToSkills
                )
            }

            SettingsSection("About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Clawmer",
                    subtitle = "Version 1.0.0",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    Column {
        content()
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
