package com.stellarflux.ui.screens.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stellarflux.data.model.Skill
import com.stellarflux.data.repository.OpenClawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SkillsUiState(
    val skills: List<Skill> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SkillsViewModel(
    private val repository: OpenClawRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    init {
        loadSkills()
    }

    private fun loadSkills() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val skills = repository.getSkillsStatus()
                _uiState.value = _uiState.value.copy(skills = skills, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleSkill(skill: Skill) {
        viewModelScope.launch {
            try {
                repository.updateSkill(skill.id, !skill.enabled)
                val updated = _uiState.value.skills.map {
                    if (it.id == skill.id) it.copy(enabled = !it.enabled) else it
                }
                _uiState.value = _uiState.value.copy(skills = updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun invokeSkill(skill: Skill) {
        // Skills invocation would typically navigate to a chat and send a command
        // For now, this is a placeholder
    }

    class Factory(
        private val repository: OpenClawRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SkillsViewModel(repository) as T
        }
    }
}
