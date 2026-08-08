package com.canineai.android.presentation.upload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.upload.event.UploadEvent
import com.canineai.android.presentation.upload.event.UploadUiAction
import com.canineai.android.presentation.upload.state.DicomMetadata
import com.canineai.android.presentation.upload.state.UploadStatus
import com.canineai.android.presentation.upload.state.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: CanineRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(UploadState())
    val state: StateFlow<UploadState> = _state.asStateFlow()

    private val _uiActions = Channel<UploadUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    private var uploadJob: Job? = null

    fun onEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.LinkPatient -> _state.update { it.copy(patientId = event.patientId, patientName = event.name, apiError = null) }
            is UploadEvent.FileSelected -> handleFileSelection(event.name, event.size, event.uri)
            is UploadEvent.TriggerUpload -> startUploadSequence()
            is UploadEvent.CancelCurrentUpload -> cancelUploadSequence()
            is UploadEvent.RetryCurrentUpload -> startUploadSequence()
            is UploadEvent.ClearCompletedUploads -> clearCompleted()
            is UploadEvent.ToggleValidationWarning -> _state.update { it.copy(showValidationWarning = !it.showValidationWarning) }
            is UploadEvent.SwitchWorkspaceTab -> _state.update { it.copy(selectedTab = event.index) }
            is UploadEvent.ProceedToAIAnalysisRequested -> triggerAnalysisRouting()
            is UploadEvent.DeleteStudyRequested -> _state.update { it.copy(showDeleteConfirmation = true) }
            is UploadEvent.DeleteStudyConfirmed -> performDelete()
            is UploadEvent.DismissError -> _state.update { it.copy(apiError = null) }
        }
    }

    private fun handleFileSelection(name: String, size: String, uri: android.net.Uri?) {
        _state.update {
            it.copy(
                uploadState = UploadStatus.IDLE,
                fileName = name,
                fileSize = size,
                uri = uri,
                progress = 0f,
                dicomMetadata = null,
                validationErrors = emptyList(),
                apiError = null
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun startUploadSequence() {
        val fileName = _state.value.fileName
        val uri = _state.value.uri
        val patientId = _state.value.patientId

        if (fileName.isBlank() || uri == null) {
            _state.update { it.copy(apiError = "Choose a study before uploading.") }
            return
        }
        if (patientId.isBlank()) {
            _state.update { it.copy(apiError = "Select a patient before uploading.") }
            return
        }

        uploadJob?.cancel()
        _state.update { it.copy(uploadState = UploadStatus.UPLOADING, progress = 0f, apiError = null) }

        uploadJob = viewModelScope.launch {
            try {
                // Determine file size and read bytes
                val fileBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw RuntimeException("Unable to read selected file.")
                
                val requestBody = fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())

                _state.update { it.copy(progress = 0.5f) } // Simulating midway progress

                if (fileName.lowercase().endsWith(".zip")) {
                    repository.uploadZip(patientId, requestBody)
                } else {
                    val initResp = repository.initializeUploadSession(patientId, fileBytes.size.toLong(), 1)
                    repository.uploadChunk(initResp.id, fileName, requestBody)
                }

                _state.update {
                    it.copy(
                        uploadState = UploadStatus.COMPLETED,
                        progress = 1f,
                        dicomMetadata = DicomMetadata(
                            patientId = patientId,
                            patientName = _state.value.patientName,
                            studyUid = "study-from-upload",
                            seriesUid = "series-from-upload"
                        ) // Hardcode for routing until study API fetches it
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        uploadState = UploadStatus.FAILED,
                        progress = 0f,
                        apiError = e.message ?: "Upload failed due to unknown network error."
                    )
                }
            }
        }
    }

    private fun cancelUploadSequence() {
        uploadJob?.cancel()
        _state.update { it.copy(uploadState = UploadStatus.CANCELLED, progress = 0f, apiError = null) }
    }

    private fun clearCompleted() {
        _state.update {
            it.copy(
                uploadState = UploadStatus.IDLE,
                fileName = "",
                fileSize = "",
                progress = 0f,
                dicomMetadata = null,
                apiError = null
            )
        }
    }

    private fun triggerAnalysisRouting() {
        viewModelScope.launch {
            _uiActions.send(
                UploadUiAction.NavigateToAIAnalysis(
                    patientId = _state.value.patientId,
                    studyId = _state.value.dicomMetadata?.studyUid.orEmpty()
                )
            )
        }
    }

    private fun performDelete() {
        _state.update { it.copy(showDeleteConfirmation = false) }
        clearCompleted()
    }
}
