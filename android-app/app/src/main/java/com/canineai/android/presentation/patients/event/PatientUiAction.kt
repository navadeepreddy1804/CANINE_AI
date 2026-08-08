package com.canineai.android.presentation.patients.event

sealed class PatientUiAction {
    object NavigateBack : PatientUiAction()
    data class NavigateToPatientDetails(val patientId: String) : PatientUiAction()
    data class NavigateToUploadCBCT(val patientId: String) : PatientUiAction()
    data class NavigateToAIAnalysis(val patientId: String) : PatientUiAction()
    data class NavigateToGenerateReport(val patientId: String) : PatientUiAction()
}
