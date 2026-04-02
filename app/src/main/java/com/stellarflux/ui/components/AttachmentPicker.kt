package com.stellarflux.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttachmentPicker(
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = "Attach",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Take Photo") },
                onClick = {
                    expanded = false
                    onTakePhoto()
                },
                leadingIcon = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Choose Image") },
                onClick = {
                    expanded = false
                    onPickImage()
                },
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Attach File") },
                onClick = {
                    expanded = false
                    onPickFile()
                },
                leadingIcon = {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                }
            )
        }
    }
}
