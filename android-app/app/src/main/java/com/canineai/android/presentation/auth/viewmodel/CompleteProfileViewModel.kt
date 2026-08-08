package com.canineai.android.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.repository.CanineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun completeProfile(phone: String, roleTitle: String, hospital: String, medicalRegNo: String, onSuccess: () -> Unit) {
        _isLoading.update { true }
        _error.update { null }

        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()
                val updatedUser = currentUser.copy(
                    phone = phone.ifBlank { currentUser.phone },
                    roleTitle = roleTitle.ifBlank { currentUser.roleTitle },
                    hospital = hospital.ifBlank { currentUser.hospital },
                    medicalRegistrationNumber = medicalRegNo.ifBlank { currentUser.medicalRegistrationNumber }
                )
                repository.updateProfile(updatedUser)
                _isLoading.update { false }
                onSuccess()
            } catch (e: Exception) {
                _isLoading.update { false }
                _error.update { e.message ?: "Failed to update profile" }
            }
        }
    }
}
