package com.stellarflux.ui.screens.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stellarflux.data.model.Agent
import com.stellarflux.data.model.AgentFile
import com.stellarflux.data.repository.OpenClawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgentUiState(
    val agents: List<Agent> = emptyList(),
    val selectedAgent: Agent? = null,
    val agentFiles: List<AgentFile> = emptyList(),
    val editingFile: String? = null,
    val editingContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AgentViewModel(
    private val repository: OpenClawRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        loadAgents()
    }

    private fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val agents = repository.listAgents()
                _uiState.value = _uiState.value.copy(agents = agents, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectAgent(agent: Agent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedAgent = agent, isLoading = true)
            try {
                val files = repository.getAgentFiles(agent.id)
                _uiState.value = _uiState.value.copy(agentFiles = files, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedAgent = null,
            agentFiles = emptyList(),
            editingFile = null,
            editingContent = ""
        )
    }

    fun loadFile(fileName: String) {
        val agent = _uiState.value.selectedAgent ?: return
        viewModelScope.launch {
            try {
                val content = repository.getAgentFileContent(agent.id, fileName)
                _uiState.value = _uiState.value.copy(
                    editingFile = fileName,
                    editingContent = content
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateEditingContent(content: String) {
        _uiState.value = _uiState.value.copy(editingContent = content)
    }

    fun saveFile() {
        val agent = _uiState.value.selectedAgent ?: return
        val fileName = _uiState.value.editingFile ?: return
        viewModelScope.launch {
            try {
                repository.setAgentFileContent(agent.id, fileName, _uiState.value.editingContent)
                _uiState.value = _uiState.value.copy(editingFile = null, editingContent = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(editingFile = null, editingContent = "")
    }

    class Factory(
        private val repository: OpenClawRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgentViewModel(repository) as T
        }
    }
}
