package com.stellarflux

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stellarflux.data.ServerStore
import com.stellarflux.ui.screens.servers.ServerListScreen
import com.stellarflux.ui.screens.servers.ServerSetupScreen
import com.stellarflux.ui.theme.ClawmerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverStore = ServerStore(this)

        setContent {
            ClawmerTheme {
                var servers by rememberSaveable {
                    mutableStateOf(serverStore.getServers().map { it.id to it.url })
                }
                var webViewUrl by rememberSaveable { mutableStateOf<String?>(null) }

                when {
                    // WebView is open
                    webViewUrl != null -> {
                        WebViewScreen(
                            url = webViewUrl!!,
                            onBack = { webViewUrl = null }
                        )
                    }
                    // No servers yet — first-time setup
                    servers.isEmpty() -> {
                        ServerSetupScreen(
                            onServerAdded = { url ->
                                val entry = serverStore.addServer(url)
                                servers = serverStore.getServers().map { it.id to it.url }
                                webViewUrl = url
                            }
                        )
                    }
                    // Has servers — show list
                    else -> {
                        ServerListScreen(
                            servers = serverStore.getServers(),
                            onSelectServer = { server ->
                                webViewUrl = server.url
                            },
                            onAddServer = { url ->
                                serverStore.addServer(url)
                                servers = serverStore.getServers().map { it.id to it.url }
                            },
                            onRemoveServer = { id ->
                                serverStore.removeServer(id)
                                servers = serverStore.getServers().map { it.id to it.url }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                    }
                }
            )

            SmallFloatingActionButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(40.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = "Servers",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
