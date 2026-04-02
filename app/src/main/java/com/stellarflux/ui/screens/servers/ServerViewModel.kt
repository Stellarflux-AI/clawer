package com.stellarflux.ui.screens.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stellarflux.data.model.Server
import com.stellarflux.data.remote.ConnectionState
import com.stellarflux.data.repository.OpenClawRepository
import com.stellarflux.data.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

data class ServerUiState(
    val servers: List<Server> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

class ServerViewModel(
    private val serverRepository: ServerRepository,
    private val openClawRepository: OpenClawRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerUiState())
    val uiState: StateFlow<ServerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            serverRepository.servers.combine(openClawRepository.connectionState) { servers, connState ->
                ServerUiState(servers = servers, connectionState = connState)
            }.collect { _uiState.value = it }
        }
    }

    fun addServer(name: String, url: String, token: String) {
        viewModelScope.launch {
            serverRepository.addServer(
                Server(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    url = url,
                    authToken = token
                )
            )
        }
    }

    fun updateServer(server: Server) {
        viewModelScope.launch {
            serverRepository.updateServer(server)
        }
    }

    fun removeServer(id: String) {
        viewModelScope.launch {
            serverRepository.removeServer(id)
        }
    }

    fun setActiveServer(id: String) {
        viewModelScope.launch {
            serverRepository.setActiveServer(id)
        }
    }

    class Factory(
        private val serverRepository: ServerRepository,
        private val openClawRepository: OpenClawRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServerViewModel(serverRepository, openClawRepository) as T
        }
    }
}
