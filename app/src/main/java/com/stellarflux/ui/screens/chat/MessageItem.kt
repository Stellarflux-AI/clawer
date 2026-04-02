package com.stellarflux.ui.screens.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stellarflux.data.model.ChatMessage
import com.stellarflux.data.model.MessageRole
import com.stellarflux.data.model.ToolStatus
import com.stellarflux.ui.theme.UserBubbleDark
import com.stellarflux.ui.theme.UserBubbleLight

@Composable
fun MessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        MessageRole.USER -> UserMessage(message, modifier)
        MessageRole.ASSISTANT -> AssistantMessage(message, modifier)
        MessageRole.TOOL -> ToolMessage(message, modifier)
        MessageRole.SYSTEM -> SystemMessage(message, modifier)
    }
}

@Composable
private fun UserMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(if (isDark) UserBubbleDark else UserBubbleLight)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        SelectionContainer {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (message.isStreaming) {
            StreamingIndicator()
        }
    }
}

@Composable
private fun ToolMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    val toolUse = message.toolUse ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (toolUse.status) {
            ToolStatus.RUNNING -> Icons.Default.Build
            ToolStatus.COMPLETED -> Icons.Default.CheckCircle
            ToolStatus.ERROR -> Icons.Default.Error
        }
        val tint = when (toolUse.status) {
            ToolStatus.RUNNING -> MaterialTheme.colorScheme.primary
            ToolStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
            ToolStatus.ERROR -> MaterialTheme.colorScheme.error
        }
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = toolUse.toolName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (toolUse.status == ToolStatus.RUNNING) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SystemMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Text(
        text = message.content,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )
    Text(
        text = "\u2588",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.alpha(alpha)
    )
}
