package com.canineai.android.presentation.patients.event

sealed class PatientEvent {
    data class SearchQueryChanged(val query: String) : PatientEvent()
    data class GenderFilterChanged(val gender: String?) : PatientEvent()
    data class StatusFilterChanged(val status: String?) : PatientEvent()
    
    data class SelectPatient(val patientId: String) : PatientEvent()
    object LoadNextPage : PatientEvent()
    
    // Form Inputs
    data class FullNameChanged(val name: String) : PatientEvent()
    data class AgeChanged(val age: String) : PatientEvent()
    data class GenderChanged(val gender: String) : PatientEvent()
    data class BloodGroupChanged(val bloodGroup: String) : PatientEvent()
    data class DobChanged(val dob: String) : PatientEvent()
    data class PhoneChanged(val phone: String) : PatientEvent()
    data class EmailChanged(val email: String) : PatientEvent()
    data class NotesChanged(val notes: String) : PatientEvent()
    data class OrthodontistChanged(val doc: String) : PatientEvent()
    
    object SavePatientSubmitted : PatientEvent()
    object DeletePatientRequested : PatientEvent()
    object DeletePatientConfirmed : PatientEvent()
    object ArchivePatientRequested : PatientEvent()
    
    // UI Dialogs
    object DismissDeleteDialog : PatientEvent()
    object DismissSuccessDialog : PatientEvent()
    object DismissError : PatientEvent()
    
    // Form Edit Loader
    data class PrepareEditForm(val patientId: String) : PatientEvent()
}
