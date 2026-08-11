package com.canineai.android.presentation.patients.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
data class PatientState(
    val patientsList: List<PatientItem> = emptyList(),
    val filteredPatientsList: List<PatientItem> = emptyList(),
    
    // Search & Filtering
    val searchQuery: String = "",
    val filterGender: String? = null,
    val filterStatus: String? = null,

    // Pagination
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    
    // Details Profile View
    val selectedPatient: PatientDetails? = null,
    val patientTimeline: List<PatientTimelineItem> = emptyList(),
    val patientScans: List<PatientScanItem> = emptyList(),
    
    // Input form fields (Add / Edit)
    val inputFullName: String = "",
    val inputFullNameError: String? = null,
    val inputAge: String = "",
    val inputAgeError: String? = null,
    val inputGender: String = "Male",
    val inputBloodGroup: String = "O+",
    val inputDob: String = "",
    val inputPhone: String = "",
    val inputPhoneError: String? = null,
    val inputEmail: String = "",
    val inputEmailError: String? = null,
    val inputNotes: String = "",
    val inputOrthodontist: String = "",
    
    // UI Status Indicators
    val isLoading: Boolean = false,
    val isFormSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val createdPatient: PatientItem? = null,
    val apiError: String? = null
) {
    val isFormValid: Boolean
        get() = inputFullName.isNotBlank() && 
                inputFullNameError == null && 
                inputAge.isNotBlank() && 
                inputAgeError == null && 
                inputPhone.isNotBlank() && 
                inputPhoneError == null
}

@Immutable
data class PatientItem(
    val id: String,
    val fullName: String,
    val age: Int,
    val gender: String,
    val phone: String,
    val email: String,
    val status: String, // Active / Archived
    val lastAnalysisDate: String?
) {
    val displayId: String
        get() = com.canineai.android.util.PatientIdFormatter.format(id)
}

@Immutable
data class PatientDetails(
    val id: String,
    val fullName: String,
    val age: Int,
    val gender: String,
    val dob: String,
    val bloodGroup: String?,
    val phone: String,
    val email: String,
    val address: String,
    val emergencyContact: String,
    val medicalNotes: String,
    val orthodontist: String,
    val hospital: String,
    val registrationDate: String,
    val status: String
) {
    val displayId: String
        get() = com.canineai.android.util.PatientIdFormatter.format(id)
}

@Immutable
data class PatientTimelineItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String
)

@Immutable
data class PatientScanItem(
    val id: String,
    val studyName: String,
    val date: String,
    val size: String,
    val analysisStatus: String
)
