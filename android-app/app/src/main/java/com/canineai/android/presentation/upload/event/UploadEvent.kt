package com.canineai.android.presentation.upload.event

sealed class UploadEvent {
    data class LinkPatient(val patientId: String, val name: String) : UploadEvent()
    data class FileSelected(val name: String, val size: String, val uri: android.net.Uri? = null) : UploadEvent()
    object TriggerUpload : UploadEvent()
    object CancelCurrentUpload : UploadEvent()
    object RetryCurrentUpload : UploadEvent()
    object ClearCompletedUploads : UploadEvent()
    object ToggleValidationWarning : UploadEvent()
    data class SwitchWorkspaceTab(val index: Int) : UploadEvent()
    object ProceedToAIAnalysisRequested : UploadEvent()
    object DeleteStudyRequested : UploadEvent()
    object DeleteStudyConfirmed : UploadEvent()
    object DismissError : UploadEvent()
}
