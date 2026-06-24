package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EqProfile
import com.example.data.EqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EqViewModel(private val repository: EqRepository) : ViewModel() {
    val profiles: StateFlow<List<EqProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentLevels: StateFlow<List<Float>> = repository.currentLevels
    val currentFrequencies: StateFlow<List<Float>> = repository.currentFrequencies

    private val _selectedProfile = MutableStateFlow<EqProfile?>(null)
    val selectedProfile: StateFlow<EqProfile?> = _selectedProfile

    init {
        // Init with default flat profile if needed, or wait for DB
    }

    fun updateBand(index: Int, level: Float) {
        val current = repository.currentLevels.value.toMutableList()
        current[index] = level.coerceIn(-15f, 15f)
        repository.currentLevels.value = current
        _selectedProfile.value = null // Custom modification
    }

    fun updateFrequency(index: Int, freq: Float) {
        val current = repository.currentFrequencies.value.toMutableList()
        current[index] = freq.coerceIn(20f, 20000f)
        repository.currentFrequencies.value = current
    }

    fun selectProfile(profile: EqProfile) {
        _selectedProfile.value = profile
        repository.currentLevels.value = profile.levels
    }

    fun saveCurrentAsProfile(name: String) {
        viewModelScope.launch {
            val profile = EqProfile(name = name, levels = repository.currentLevels.value, isCustom = true)
            val id = repository.insertProfile(profile)
            _selectedProfile.value = profile.copy(id = id.toInt())
        }
    }

    fun deleteProfile(profile: EqProfile) {
        viewModelScope.launch {
            repository.deleteProfileById(profile.id)
            if (_selectedProfile.value?.id == profile.id) {
                _selectedProfile.value = null
            }
        }
    }

    fun createDefaultProfilesIfNeeded(existingProfiles: List<EqProfile>) {
        if (existingProfiles.isEmpty()) {
            viewModelScope.launch {
                repository.insertProfile(EqProfile(name = "Flat", isCustom = false, levels = List(30) { 0f }))
                repository.insertProfile(EqProfile(name = "Bass Boost", isCustom = false, levels = List(30) { i ->
                    when {
                        i < 5 -> 10f
                        i < 10 -> 5f
                        else -> 0f
                    }
                }))
                repository.insertProfile(EqProfile(name = "Vocal Boost", isCustom = false, levels = List(30) { i ->
                    when (i) {
                        in 10..20 -> 8f
                        else -> 0f
                    }
                }))
            }
        }
    }
}

class EqViewModelFactory(private val repository: EqRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EqViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EqViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
