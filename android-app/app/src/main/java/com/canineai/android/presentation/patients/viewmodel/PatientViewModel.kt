package com.canineai.android.presentation.patients.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.network.PatientDto
import com.canineai.android.data.network.NetworkErrorResolver
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.patients.event.PatientEvent
import com.canineai.android.presentation.patients.event.PatientUiAction
import com.canineai.android.presentation.patients.state.PatientDetails
import com.canineai.android.presentation.patients.state.PatientItem
import com.canineai.android.presentation.patients.state.PatientState
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
class PatientViewModel @Inject constructor(
    private val repository: CanineRepository,
    private val sessionManager: com.canineai.android.data.local.SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PatientState())
    val state: StateFlow<PatientState> = _state.asStateFlow()

    private val _uiActions = Channel<PatientUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    init {
        val fullName = sessionManager.getFullName()
        val email = sessionManager.getEmail()
        val docName = if (!fullName.isNullOrBlank()) fullName else if (!email.isNullOrBlank()) email else "Dr. John Smith"
        val formattedDoc = if (docName.startsWith("Dr.")) docName else "Dr. $docName"
        _state.update { it.copy(inputOrthodontist = formattedDoc) }
        loadPatientsList()
    }

    fun onEvent(event: PatientEvent) {
        when (event) {
            is PatientEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is PatientEvent.GenderFilterChanged -> updateGenderFilter(event.gender)
            is PatientEvent.StatusFilterChanged -> updateStatusFilter(event.status)
            is PatientEvent.SelectPatient -> loadPatientDetails(event.patientId)
            is PatientEvent.FullNameChanged -> validateFullName(event.name)
            is PatientEvent.AgeChanged -> validateAge(event.age)
            is PatientEvent.GenderChanged -> _state.update { it.copy(inputGender = event.gender) }
            is PatientEvent.BloodGroupChanged -> _state.update { it.copy(inputBloodGroup = event.bloodGroup) }
            is PatientEvent.DobChanged -> _state.update { it.copy(inputDob = event.dob) }
            is PatientEvent.PhoneChanged -> validatePhone(event.phone)
            is PatientEvent.EmailChanged -> validateEmail(event.email)
            is PatientEvent.NotesChanged -> _state.update { it.copy(inputNotes = event.notes) }
            is PatientEvent.OrthodontistChanged -> _state.update { it.copy(inputOrthodontist = event.doc) }
            is PatientEvent.SavePatientSubmitted -> savePatientForm()
            is PatientEvent.PrepareEditForm -> fillFormForEdit(event.patientId)
            is PatientEvent.DeletePatientRequested -> _state.update { it.copy(showDeleteConfirmation = true) }
            is PatientEvent.DeletePatientConfirmed -> performDelete()
            is PatientEvent.DismissDeleteDialog -> _state.update { it.copy(showDeleteConfirmation = false) }
            is PatientEvent.DismissSuccessDialog -> _state.update { it.copy(showSuccessDialog = false) }
            is PatientEvent.DismissError -> _state.update { it.copy(apiError = null) }
            is PatientEvent.LoadNextPage -> loadNextPage()
            else -> {}
        }
    }

    private fun loadPatientsList() {
        _state.update { it.copy(isLoading = true, apiError = null, currentPage = 0, isLastPage = false) }
        viewModelScope.launch {
            try {
                val list = repository.getPatients(
                    search = _state.value.searchQuery.takeIf { it.isNotBlank() },
                    gender = _state.value.filterGender,
                    status = _state.value.filterStatus,
                    page = 0,
                    size = 20
                )
                _state.update { it.copy(
                    patientsList = list, 
                    filteredPatientsList = list, 
                    isLoading = false,
                    isLastPage = list.size < 20
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }

    private fun loadNextPage() {
        if (_state.value.isLoading || _state.value.isLoadingNextPage || _state.value.isLastPage) return

        val nextPage = _state.value.currentPage + 1
        _state.update { it.copy(isLoadingNextPage = true, apiError = null) }
        viewModelScope.launch {
            try {
                val list = repository.getPatients(
                    search = _state.value.searchQuery.takeIf { it.isNotBlank() },
                    gender = _state.value.filterGender,
                    status = _state.value.filterStatus,
                    page = nextPage,
                    size = 20
                )
                
                val currentList = _state.value.patientsList
                val newList = currentList + list

                _state.update { it.copy(
                    patientsList = newList, 
                    filteredPatientsList = newList, 
                    isLoadingNextPage = false,
                    currentPage = nextPage,
                    isLastPage = list.size < 20
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingNextPage = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        loadPatientsList()
    }

    private fun updateGenderFilter(gender: String?) {
        _state.update { it.copy(filterGender = gender) }
        loadPatientsList()
    }

    private fun updateStatusFilter(status: String?) {
        _state.update { it.copy(filterStatus = status) }
        loadPatientsList()
    }

    private fun loadPatientDetails(patientId: String) {
        _state.update { it.copy(isLoading = true, apiError = null) }
        viewModelScope.launch {
            try {
                val details = repository.getPatientDetails(patientId)
                val scans = repository.getPatientScans(patientId)
                val timeline = repository.getPatientTimeline(patientId)
                _state.update {
                    it.copy(
                        selectedPatient = details,
                        patientScans = scans,
                        patientTimeline = timeline,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }

    private fun validateFullName(name: String) {
        val error = if (name.isBlank()) "Full name is required" else null
        _state.update { it.copy(inputFullName = name, inputFullNameError = error) }
    }

    private fun validateAge(age: String) {
        val error = when {
            age.isBlank() -> "Age is required"
            age.toIntOrNull() == null -> "Age must be a number"
            else -> null
        }
        _state.update { it.copy(inputAge = age, inputAgeError = error) }
    }

    private fun validatePhone(phone: String) {
        val error = if (phone.isBlank()) "Phone number is required" else null
        _state.update { it.copy(inputPhone = phone, inputPhoneError = error) }
    }

    private fun validateEmail(email: String) {
        val error = when {
            email.isBlank() -> "Email address is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email address format"
            else -> null
        }
        _state.update { it.copy(inputEmail = email, inputEmailError = error) }
    }

    private fun fillFormForEdit(patientId: String) {
        _state.update { it.copy(isLoading = true, apiError = null) }
        viewModelScope.launch {
            try {
                val details = repository.getPatientDetails(patientId)
                _state.update {
                    it.copy(
                        inputFullName = details.fullName,
                        inputAge = details.age.toString(),
                        inputGender = details.gender,
                        inputBloodGroup = details.bloodGroup ?: "O+",
                        inputDob = details.dob,
                        inputPhone = details.phone,
                        inputEmail = details.email,
                        inputNotes = details.medicalNotes,
                        inputOrthodontist = details.orthodontist,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }

    private fun savePatientForm() {
        if (!_state.value.isFormValid || _state.value.isFormSaving) return

        _state.update { it.copy(isFormSaving = true, apiError = null) }
        viewModelScope.launch {
            try {
                val computedDob = if (_state.value.inputDob.isNotBlank()) {
                    _state.value.inputDob
                } else {
                    val ageVal = _state.value.inputAge.toIntOrNull() ?: 30
                    try {
                        java.time.LocalDate.now().minusYears(ageVal.toLong()).toString()
                    } catch (e: Exception) {
                        "1990-01-01"
                    }
                }

                val patientDto = PatientDto(
                    id = _state.value.selectedPatient?.id,
                    hospitalPatientId = null,
                    fullName = _state.value.inputFullName,
                    age = _state.value.inputAge.toIntOrNull() ?: 0,
                    gender = if (_state.value.inputGender.isNotBlank()) _state.value.inputGender.trim().uppercase() else "FEMALE",
                    dob = computedDob,
                    dateOfBirth = computedDob,
                    phone = _state.value.inputPhone,
                    email = _state.value.inputEmail,
                    status = "ACTIVE",
                    orthodontist = _state.value.inputOrthodontist,
                    bloodGroup = _state.value.inputBloodGroup,
                    medicalNotes = _state.value.inputNotes,
                    registrationDate = null,
                    studies = null,
                    reports = null,
                    hospital = "Metro Dental Diagnostics"
                )
                val savedPatient = repository.savePatient(patientDto)
                val newItem = PatientItem(
                    id = savedPatient.hospitalPatientId ?: savedPatient.id ?: "0",
                    fullName = savedPatient.fullName.orEmpty(),
                    age = savedPatient.age ?: 0,
                    gender = savedPatient.gender.orEmpty(),
                    phone = savedPatient.phone.orEmpty(),
                    email = savedPatient.email.orEmpty(),
                    status = savedPatient.status ?: "Active",
                    lastAnalysisDate = null
                )
                _state.update { it.copy(isFormSaving = false, showSuccessDialog = true, createdPatient = newItem) }
                loadPatientsList()
            } catch (e: Exception) {
                _state.update { it.copy(isFormSaving = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }

    private fun performDelete() {
        val patientId = _state.value.selectedPatient?.id ?: return
        if (_state.value.isLoading) return
        
        _state.update { it.copy(showDeleteConfirmation = false, isLoading = true, apiError = null) }
        viewModelScope.launch {
            try {
                repository.deletePatient(patientId)
                _state.update { it.copy(isLoading = false) }
                _uiActions.send(PatientUiAction.NavigateBack)
                loadPatientsList()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, apiError = NetworkErrorResolver.resolve(e)) }
            }
        }
    }
}
