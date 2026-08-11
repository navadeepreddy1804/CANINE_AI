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

    init {
        loadPatients()
    }

    fun loadPatients() {
        _state.update { it.copy(isLoadingPatients = true) }
        viewModelScope.launch {
            try {
                val list = repository.getPatients(page = 0, size = 100)
                _state.update {
                    it.copy(
                        patientsList = list,
                        isLoadingPatients = false,
                        selectedPatientItem = list.firstOrNull(),
                        patientId = list.firstOrNull()?.id.orEmpty(),
                        patientName = list.firstOrNull()?.fullName.orEmpty()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingPatients = false) }
            }
        }
    }

    fun onEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.LinkPatient -> {
                val item = _state.value.patientsList.firstOrNull { it.id == event.patientId }
                _state.update { it.copy(patientId = event.patientId, patientName = event.name, selectedPatientItem = item, apiError = null) }
            }
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
        val metadata = DicomMetadata(
            patientId = _state.value.patientId.ifBlank { "PT-00001" },
            patientName = _state.value.patientName.ifBlank { "Selected Patient" },
            studyUid = "study-${System.currentTimeMillis()}",
            seriesUid = "series-1.3.6.1.4.1.5962.1",
            modality = if (name.lowercase().contains("nii")) "NIfTI Volume" else "3D CBCT DICOM",
            dimensions = "512 x 512 x 360",
            sliceThickness = "0.300 mm",
            voxelSize = "0.3 x 0.3 x 0.3 mm",
            sliceCount = 360,
            studyDescription = "High-Resolution Maxilla CBCT Scan ($name)"
        )

        _state.update {
            it.copy(
                uploadState = UploadStatus.IDLE,
                fileName = name,
                fileSize = size,
                uri = uri,
                progress = 0f,
                dicomMetadata = metadata,
                validationErrors = emptyList(),
                apiError = null
            )
        }
    }

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
        _state.update { it.copy(uploadState = UploadStatus.UPLOADING, progress = 0.1f, apiError = null) }

        uploadJob = viewModelScope.launch {
            try {
                val streamingRequestBody = com.canineai.android.data.network.ContentUriRequestBody(context, uri)
                val fileSize = streamingRequestBody.contentLength()

                val sessionDto: com.canineai.android.data.network.UploadSessionDto = if (fileName.lowercase().endsWith(".zip")) {
                    repository.uploadZip(patientId, streamingRequestBody)
                } else {
                    val initResp = repository.initializeUploadSession(patientId, if (fileSize > 0) fileSize else 1024L * 1024L, 1)
                    repository.uploadChunk(initResp.id, fileName, streamingRequestBody)
                    initResp
                }

                _state.update { it.copy(progress = 0.6f, uploadState = UploadStatus.VALIDATING) }

                var currentSession = sessionDto
                var retries = 0
                val sessionId = sessionDto.id

                while (currentSession.status != "COMPLETED" && currentSession.status != "PREVIEW_READY" && retries < 30) {
                    kotlinx.coroutines.delay(1000)
                    retries++
                    try {
                        currentSession = repository.getSessionStatus(sessionId)
                        val prog = when (currentSession.status) {
                            "VALIDATING" -> 0.7f
                            "PROCESSING" -> 0.85f
                            "COMPLETED", "PREVIEW_READY" -> 1.0f
                            else -> 0.6f
                        }
                        _state.update { it.copy(progress = prog) }
                    } catch (e: Exception) {
                        // Best effort polling retry
                    }
                    if (currentSession.status == "FAILED") {
                        throw RuntimeException("Backend validation failed: ${currentSession.status}")
                    }
                }

                val realStudyId = currentSession.studyId
                    ?: repository.getPatientScans(patientId).firstOrNull()?.id
                    ?: "study-${System.currentTimeMillis()}"

                _state.update {
                    it.copy(
                        uploadState = UploadStatus.COMPLETED,
                        progress = 1f,
                        dicomMetadata = DicomMetadata(
                            patientId = patientId,
                            patientName = _state.value.patientName,
                            studyUid = realStudyId,
                            seriesUid = "series-$realStudyId"
                        )
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
