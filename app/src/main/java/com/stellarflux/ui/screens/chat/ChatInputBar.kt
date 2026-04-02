package com.stellarflux.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stellarflux.ui.components.AttachmentPicker
import com.stellarflux.ui.components.VoiceRecordButton

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onAbort: () -> Unit,
    isSending: Boolean,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AttachmentPicker(
                    onPickFile = onPickFile,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto
                )

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 160.dp),
                    placeholder = {
                        Text(
                            "Message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank() && !isSending) {
                                onSendMessage(text.trim())
                                text = ""
                            }
                        }
                    ),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                AnimatedVisibility(visible = text.isBlank() && !isSending) {
                    VoiceRecordButton(
                        isRecording = isRecording,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording
                    )
                }

                if (isSending) {
                    IconButton(
                        onClick = onAbort,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    AnimatedVisibility(visible = text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onSendMessage(text.trim())
                                    text = ""
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
