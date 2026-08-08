package com.canineai.android.presentation.analysis.event

sealed class AnalysisUiAction {
    data class NavigateToReports(val patientId: String, val studyId: String) : AnalysisUiAction()
    object RouteBackToUpload : AnalysisUiAction()
}
