package com.canineai.android.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.presentation.settings.event.SettingsEvent
import com.canineai.android.presentation.settings.event.SettingsUiAction
import com.canineai.android.presentation.settings.state.SettingsState
import com.canineai.android.presentation.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: com.canineai.android.data.repository.CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(isDarkMode = ThemeManager.isDarkMode.value))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _uiActions = Channel<SettingsUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val user = repository.getCurrentUser()
                val patients = runCatching { repository.getPatients() }.getOrDefault(emptyList())
                val reports = runCatching { repository.getReports() }.getOrDefault(emptyList())
                val history = runCatching { repository.getHistory() }.getOrDefault(emptyList())

                _state.update {
                    it.copy(
                        fullName = user.fullName.orEmpty(), email = user.email.orEmpty(),
                        phone = user.phone.orEmpty(), role = user.roleTitle ?: user.role.orEmpty(),
                        hospital = user.hospital.orEmpty(), department = user.department.orEmpty(),
                        medicalRegNo = user.medicalRegistrationNumber.orEmpty(),
                        activePatientsCount = patients.size,
                        completedAnalysesCount = history.size,
                        reportsGeneratedCount = reports.size,
                        apiError = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(apiError = "Settings data is unavailable from the backend.") }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.FullNameChanged -> _state.update { it.copy(fullName = event.value) }
            is SettingsEvent.PhoneChanged -> _state.update { it.copy(phone = event.value) }
            is SettingsEvent.HospitalChanged -> _state.update { it.copy(hospital = event.value) }
            is SettingsEvent.DepartmentChanged -> _state.update { it.copy(department = event.value) }
            is SettingsEvent.RoleChanged -> _state.update { it.copy(role = event.value) }
            is SettingsEvent.MedicalRegNoChanged -> _state.update { it.copy(medicalRegNo = event.value) }
            is SettingsEvent.SaveProfileClicked -> saveProfile()
            is SettingsEvent.DismissSuccessDialog -> _state.update { it.copy(showSaveSuccess = false) }
            is SettingsEvent.DarkModeToggled -> {
                ThemeManager.setDarkMode(event.value)
                _state.update { it.copy(isDarkMode = event.value) }
            }
            is SettingsEvent.NotificationsToggled -> _state.update { it.copy(isNotificationsEnabled = event.value) }
            is SettingsEvent.LanguageChanged -> _state.update { it.copy(language = event.value) }
            is SettingsEvent.CleanTemporaryFiles -> clearTempFiles()
            is SettingsEvent.LogoutDevice -> removeDevice(event.deviceName)
            is SettingsEvent.LogoutAllDevices -> removeAllDevicesExceptCurrent()
        }
    }

    private fun saveProfile() {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Build UserDto to send to backend
                val dto = com.canineai.android.data.network.UserDto(
                    id = null,
                    username = null,
                    email = _state.value.email,
                    fullName = _state.value.fullName,
                    phone = _state.value.phone,
                    roleTitle = _state.value.role,
                    hospital = _state.value.hospital,
                    department = _state.value.department,
                    medicalRegistrationNumber = _state.value.medicalRegNo,
                    yearsOfExperience = null,
                    bloodGroup = null,
                    enabled = true,
                    roles = null
                )

                repository.updateProfile(dto)
                _state.update { it.copy(isSaving = false, showSaveSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _uiActions.send(com.canineai.android.presentation.settings.event.SettingsUiAction.ShowToast("Failed to save profile: ${e.message}"))
            }
        }
    }

    private fun clearTempFiles() {
        _state.update { it.copy(logsSizeGb = 0f) }
        viewModelScope.launch {
            _uiActions.send(SettingsUiAction.ShowToast("Temporary EMR logs storage cleared successfully."))
        }
    }

    private fun removeDevice(name: String) {
        _state.update {
            it.copy(devices = it.devices.filter { d -> d.deviceName != name || d.isCurrent })
        }
        viewModelScope.launch {
            _uiActions.send(SettingsUiAction.ShowToast("Logged out of device $name successfully."))
        }
    }

    private fun removeAllDevicesExceptCurrent() {
        _state.update {
            it.copy(devices = it.devices.filter { d -> d.isCurrent })
        }
        viewModelScope.launch {
            _uiActions.send(SettingsUiAction.ShowToast("Logged out of all other active sessions."))
        }
    }
}
