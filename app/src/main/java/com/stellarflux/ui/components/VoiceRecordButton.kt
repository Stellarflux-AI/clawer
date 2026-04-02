package com.stellarflux.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = tween(200),
        label = "voice_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "voice_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "voice_icon"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .background(bgColor, CircleShape)
            .pointerInput(isRecording) {
                detectTapGestures(
                    onTap = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Start recording",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
