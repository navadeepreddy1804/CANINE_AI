package com.canineai.android.presentation.upload.event

sealed class UploadUiAction {
    data class NavigateToAIAnalysis(val patientId: String, val studyId: String) : UploadUiAction()
    object NavigateBack : UploadUiAction()
}
