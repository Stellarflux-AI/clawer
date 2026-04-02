package com.stellarflux.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stellarflux.data.model.AiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    currentModel: String,
    models: List<AiModel>,
    onModelSelected: (AiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    TextButton(
        onClick = { showSheet = true },
        modifier = modifier
    ) {
        Text(
            text = currentModel.ifEmpty { "Select Model" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = "Select model",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Text(
                "Select Model",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                items(models) { model ->
                    ListItem(
                        headlineContent = { Text(model.name) },
                        supportingContent = if (model.provider.isNotEmpty()) {
                            { Text(model.provider, style = MaterialTheme.typography.bodySmall) }
                        } else null,
                        trailingContent = if (model.id == currentModel || model.name == currentModel) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        modifier = Modifier.clickable {
                            onModelSelected(model)
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}
