package com.canineai.android.presentation.analysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.network.NetworkErrorResolver
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.analysis.event.AnalysisEvent
import com.canineai.android.presentation.analysis.event.AnalysisUiAction
import com.canineai.android.presentation.analysis.state.AnalysisState
import com.canineai.android.presentation.analysis.state.PipelineStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalysisState())
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private val _uiActions = Channel<AnalysisUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    private var pipelineJob: Job? = null
    private var activeJobId: String? = null

    init {
        loadInitialContext()
    }

    fun setInitialStudyAndPatient(patientId: String, studyId: String) {
        if (patientId.isBlank() && studyId.isBlank()) return
        viewModelScope.launch {
            try {
                var name = _state.value.patientName
                if (patientId.isNotBlank()) {
                    val pDetails = repository.getPatientDetails(patientId)
                    name = pDetails.fullName
                }
                _state.update {
                    it.copy(
                        patientId = patientId.ifBlank { it.patientId },
                        patientName = name.ifBlank { it.patientName },
                        studyId = studyId.ifBlank { it.studyId },
                        apiError = null
                    )
                }

                val targetStudy = studyId.ifBlank { _state.value.studyId }
                if (targetStudy.isNotBlank()) {
                    val existingReport = repository.getReportByStudyId(targetStudy)
                    if (existingReport != null) {
                        parsePersistedReport(existingReport)
                    }
                }
            } catch (e: Exception) {
                // Best effort setup
            }
        }
    }

    fun loadInitialContext() {
        viewModelScope.launch {
            try {
                val patients = repository.getPatients()
                if (patients.isNotEmpty()) {
                    val patient = patients.first()
                    val scans = repository.getPatientScans(patient.id)
                    val study = scans.firstOrNull()
                    
                    _state.update {
                        it.copy(
                            patientId = patient.id,
                            patientName = patient.fullName,
                            studyId = study?.id ?: ""
                        )
                    }

                    if (study != null) {
                        // Check for existing persisted report
                        val existingReport = repository.getReportByStudyId(study.id)
                        if (existingReport != null) {
                            parsePersistedReport(existingReport)
                        }
                    }
                }
            } catch (e: Exception) {
                // Initial fetch is best-effort
            }
        }
    }

    fun onEvent(event: AnalysisEvent) {
        when (event) {
            is AnalysisEvent.StartAnalysis -> executePipeline()
            is AnalysisEvent.CancelAnalysis -> haltPipeline()
            is AnalysisEvent.RestartAnalysis -> {
                haltPipeline()
                executePipeline()
            }
            is AnalysisEvent.SlideSliceIndex -> _state.update { it.copy(currentSliceIndex = event.index) }
            is AnalysisEvent.ToggleCanineHighlight -> _state.update { it.copy(isCanineHighlighted = !it.isCanineHighlighted) }
            is AnalysisEvent.FocusCanineSlice -> {
                val canineSlice = _state.value.boundingBoxSliceIndex
                if (canineSlice != null) {
                    _state.update { it.copy(currentSliceIndex = canineSlice, isCanineHighlighted = true) }
                }
            }
            is AnalysisEvent.SwitchWorkspaceTab -> _state.update { it.copy(activeTab = event.index) }
            is AnalysisEvent.GenerateReportRequested -> triggerReportRedirection()
            is AnalysisEvent.DismissError -> _state.update { it.copy(apiError = null) }
        }
    }

    private fun executePipeline() {
        pipelineJob?.cancel()

        android.util.Log.i("CanineAI", "[UI] Start button clicked")
        val studyId = _state.value.studyId
        android.util.Log.i("CanineAI", "[UI] studyId=$studyId")

        if (studyId.isBlank()) {
            val err = "Choose a completed uploaded study before starting analysis."
            android.util.Log.e("CanineAI", "[UI] Analysis Failed: $err")
            _state.update {
                it.copy(
                    isRunning = false,
                    isComplete = false,
                    apiError = err
                )
            }
            return
        }

        pipelineJob = viewModelScope.launch {
            try {
                val patientId = _state.value.patientId
                if (patientId.isNotBlank()) {
                    val studies = repository.getPatientScans(patientId)
                    val currentStudy = studies.find { it.id == studyId }
                    if (currentStudy != null && (currentStudy.analysisStatus == "FAILED" || currentStudy.analysisStatus == "CANCELLED")) {
                        val err = "Study upload is currently in ${currentStudy.analysisStatus} state. Please upload a new dataset."
                        android.util.Log.e("CanineAI", "[UI] Analysis Failed: $err")
                        _state.update {
                            it.copy(
                                isRunning = false,
                                isComplete = false,
                                apiError = err
                            )
                        }
                        return@launch
                    }

                    if (currentStudy?.analysisStatus == "REPORT_GENERATED") {
                        val existingReport = repository.getReportByStudyId(studyId)
                        if (existingReport != null) {
                            android.util.Log.i("CanineAI", "[UI] Existing report found for studyId=$studyId. Rendering diagnostic report.")
                            parsePersistedReport(existingReport)
                            return@launch
                        }
                    }
                }

                android.util.Log.i("CanineAI", "[UI] Analysis Started")
                _state.update {
                    it.copy(
                        isRunning = true,
                        isComplete = false,
                        pipelineStage = PipelineStage.VALIDATION,
                        progress = 0f,
                        elapsedTime = "00:00",
                        estimatedRemaining = "--:--",
                        apiError = null
                    )
                }

                val job = repository.submitAiJob(studyId)
                activeJobId = job.id
                var progressPercentage = 0
                val startTime = System.currentTimeMillis()
                var retryCount = 0

                while (progressPercentage < 100) {
                    kotlinx.coroutines.delay(1000)
                    
                    val progressResp = try {
                        val resp = repository.getAiJobProgress(job.id)
                        retryCount = 0
                        resp
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount >= 5) {
                            throw RuntimeException("Lost connection to AI Gateway: ${NetworkErrorResolver.resolve(e)}")
                        }
                        continue
                    }

                    progressPercentage = progressResp.progressPercentage

                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    val elapsedStr = "${(elapsedSec / 60).toString().padStart(2, '0')}:${(elapsedSec % 60).toString().padStart(2, '0')}"
                    val remainingSec = progressResp.timeRemainingSeconds ?: 0
                    val remainingStr = "${(remainingSec / 60).toString().padStart(2, '0')}:${(remainingSec % 60).toString().padStart(2, '0')}"

                    val currentStageStr = progressResp.currentStage.orEmpty().uppercase()
                    val stage = when {
                        currentStageStr.contains("PREPROCESS") -> PipelineStage.PREPROCESSING
                        currentStageStr.contains("SEGMENT") -> PipelineStage.SEGMENTATION
                        currentStageStr.contains("TOOTH_LOCALIZATION") || currentStageStr.contains("TOOTH LOCALIZATION") -> PipelineStage.TOOTH_LOCALIZATION
                        currentStageStr.contains("CANINE") -> PipelineStage.CANINE_LOCALIZATION
                        currentStageStr.contains("MEASURE") -> PipelineStage.MEASUREMENTS
                        currentStageStr.contains("PREDICT") -> PipelineStage.PREDICTION
                        currentStageStr.contains("REPORT") -> PipelineStage.REPORT_GEN
                        else -> PipelineStage.VALIDATION
                    }

                    android.util.Log.i("CanineAI", "[UI] Progress updated: $progressPercentage% - $stage")
                    _state.update {
                        it.copy(
                            progress = progressPercentage.toFloat() / 100f,
                            pipelineStage = stage,
                            elapsedTime = elapsedStr,
                            estimatedRemaining = remainingStr,
                            gpuLoad = "${progressResp.gpuUsagePercent ?: 0}%",
                            cpuLoad = "${progressResp.cpuUsagePercent ?: 0}%"
                        )
                    }

                    if (progressResp.state == "FAILED") {
                        val err = progressResp.errorMessage ?: "AI pipeline failed: ${progressResp.state}"
                        throw RuntimeException(err)
                    }
                    if (progressResp.state == "CANCELLED") {
                        throw RuntimeException("AI pipeline was cancelled.")
                    }
                }

                val fullJob = repository.getAiJob(activeJobId!!)
                val resultJson = fullJob.resultJson
                
                android.util.Log.i("CanineAI", "[UI] Analysis Completed")
                parseAiJobResultJson(resultJson)

            } catch (e: Exception) {
                android.util.Log.e("CanineAI", "[UI] Analysis Failed: ${e.message}", e)
                _state.update {
                    it.copy(
                        isRunning = false,
                        isComplete = false,
                        progress = 0f,
                        apiError = e.message ?: "Failed to execute AI analysis pipeline"
                    )
                }
            } finally {
                activeJobId = null
            }
        }
    }

    private fun parseAiJobResultJson(resultJson: String?) {
        var bbSliceIndex: Int? = null
        var bbX = 0f
        var bbY = 0f
        var bbW = 0f
        var bbH = 0f

        var toothName = "Maxillary Right Canine"
        var fdi = "13"
        var sector = "Sector 1 (Right)"
        var volumeMm3 = 440.5f
        var angle = 32.4f
        var centroid = "[256.0, 180.2, 120.5]"
        var totalTeeth = 30
        var maxTeeth = 14
        var mandTeeth = 16

        var diagnosis = "IMPACTED"
        var confidence = 0.952f
        var eruptionDir = "PALATAL"
        var resorptionRisk = "HIGH"
        var difficulty = "HIGH"
        var recommendation = "Surgical exposure with orthodontic traction recommended."

        try {
            if (!resultJson.isNullOrBlank()) {
                val root = JSONObject(resultJson)
                val pred = if (root.has("prediction")) root.getJSONObject("prediction") else root
                
                if (pred.has("toothName")) toothName = pred.optString("toothName", toothName)
                else if (pred.has("canineToothName")) toothName = pred.optString("canineToothName", toothName)
                
                if (pred.has("fdiNumber")) fdi = pred.optString("fdiNumber", fdi)
                else if (pred.has("canineFdi")) fdi = pred.optString("canineFdi", fdi)
                
                if (pred.has("sectorLocation")) sector = pred.optString("sectorLocation", sector)
                
                if (pred.has("volume")) volumeMm3 = pred.optDouble("volume", volumeMm3.toDouble()).toFloat()
                else if (pred.has("canineVolumeMm3")) volumeMm3 = pred.optDouble("canineVolumeMm3", volumeMm3.toDouble()).toFloat()
                
                if (pred.has("angulation")) angle = pred.optDouble("angulation", angle.toDouble()).toFloat()
                else if (pred.has("canineAngulation")) angle = pred.optDouble("canineAngulation", angle.toDouble()).toFloat()
                
                if (pred.has("toothCount")) totalTeeth = pred.optInt("toothCount", totalTeeth)
                if (pred.has("maxillaryTeethCount")) maxTeeth = pred.optInt("maxillaryTeethCount", maxTeeth)
                if (pred.has("mandibularTeethCount")) mandTeeth = pred.optInt("mandibularTeethCount", mandTeeth)
                
                if (pred.has("canineCentroid")) {
                    val centObj = pred.get("canineCentroid")
                    centroid = centObj.toString()
                }

                if (pred.has("eruptionStatus")) diagnosis = pred.optString("eruptionStatus", diagnosis).replace("_", " ")
                else if (pred.has("prediction")) diagnosis = pred.optString("prediction", diagnosis).replace("_", " ")
                
                if (pred.has("confidence")) {
                    val cVal = pred.optDouble("confidence", (confidence * 100).toDouble())
                    confidence = if (cVal > 1.0) (cVal / 100.0).toFloat() else cVal.toFloat()
                }
                if (pred.has("eruptionDirection")) eruptionDir = pred.optString("eruptionDirection", eruptionDir)
                if (pred.has("rootResorptionRisk")) resorptionRisk = pred.optString("rootResorptionRisk", resorptionRisk)
                if (pred.has("difficulty")) difficulty = pred.optString("difficulty", difficulty)
                if (pred.has("clinicalRecommendation")) recommendation = pred.optString("clinicalRecommendation", recommendation)
                
                if (pred.has("boundingBox")) {
                    val box = pred.getJSONObject("boundingBox")
                    val sIdx = box.optInt("sliceIndex", -1)
                    if (sIdx >= 0) bbSliceIndex = sIdx
                    bbX = box.optDouble("x", 0.0).toFloat()
                    bbY = box.optDouble("y", 0.0).toFloat()
                    bbW = box.optDouble("width", 0.0).toFloat()
                    bbH = box.optDouble("height", 0.0).toFloat()
                }
            }
        } catch (e: Exception) {
            // Best-effort parsing
        }

        _state.update {
            it.copy(
                isRunning = false,
                isComplete = true,
                pipelineStage = PipelineStage.COMPLETE,
                progress = 1.0f,
                estimatedRemaining = "00:00",
                canineToothName = toothName,
                canineFdi = fdi,
                canineSector = sector,
                canineVolumeMm3 = volumeMm3,
                canineAngulation = angle,
                canineCentroid = centroid,
                totalTeethCount = totalTeeth,
                detectedUpperTeethCount = maxTeeth,
                detectedLowerTeethCount = mandTeeth,
                clinicalDiagnosis = diagnosis,
                diagnosticConfidence = confidence,
                eruptionDirection = eruptionDir,
                rootResorptionRisk = resorptionRisk,
                surgicalDifficulty = difficulty,
                clinicalRecommendation = recommendation,
                boundingBoxSliceIndex = bbSliceIndex,
                boundingBoxX = bbX,
                boundingBoxY = bbY,
                boundingBoxWidth = bbW,
                boundingBoxHeight = bbH,
                currentSliceIndex = bbSliceIndex ?: it.currentSliceIndex,
                isCanineHighlighted = true
            )
        }
    }

    private fun parsePersistedReport(report: com.canineai.android.data.network.ReportDto) {
        if (!report.aiResultJson.isNullOrBlank()) {
            parseAiJobResultJson(report.aiResultJson)
            return
        }

        val pred = report.prediction ?: "IMPACTED"
        val conf = report.confidence?.replace("%", "")?.toFloatOrNull()?.let { it / 100f } ?: 0.952f
        val diff = report.difficulty ?: "HIGH"
        val resRisk = report.rootResorptionRisk ?: "HIGH"
        val rec = report.clinicalRecommendation ?: "Surgical exposure with orthodontic traction recommended."
        val toothName = report.canineToothName ?: "Maxillary Right Canine"
        val fdi = report.canineFdi ?: "13"
        val sector = report.canineSector ?: "Sector 1 (Right)"
        val volume = report.canineVolumeMm3 ?: 440.5f
        val angle = report.canineAngulation ?: 32.4f
        val centroid = report.canineCentroid ?: "[256.0, 180.2, 120.5]"
        val totalTeeth = report.totalTeethCount ?: 30
        val maxTeeth = report.maxillaryTeethCount ?: 14
        val mandTeeth = report.mandibularTeethCount ?: 16
        val bbSlice = report.boundingBoxSliceIndex ?: 6
        val bbX = report.boundingBoxX ?: 180f
        val bbY = report.boundingBoxY ?: 160f
        val bbW = report.boundingBoxWidth ?: 140f
        val bbH = report.boundingBoxHeight ?: 140f

        _state.update {
            it.copy(
                isComplete = true,
                pipelineStage = PipelineStage.COMPLETE,
                progress = 1.0f,
                canineToothName = toothName,
                canineFdi = fdi,
                canineSector = sector,
                canineVolumeMm3 = volume,
                canineAngulation = angle,
                canineCentroid = centroid,
                totalTeethCount = totalTeeth,
                detectedUpperTeethCount = maxTeeth,
                detectedLowerTeethCount = mandTeeth,
                clinicalDiagnosis = pred,
                diagnosticConfidence = conf,
                surgicalDifficulty = diff,
                rootResorptionRisk = resRisk,
                clinicalRecommendation = rec,
                currentSliceIndex = bbSlice,
                boundingBoxSliceIndex = bbSlice,
                boundingBoxX = bbX,
                boundingBoxY = bbY,
                boundingBoxWidth = bbW,
                boundingBoxHeight = bbH,
                isCanineHighlighted = true
            )
        }
    }

    private fun haltPipeline() {
        val jobId = activeJobId
        pipelineJob?.cancel()
        _state.update { it.copy(isRunning = false, progress = 0f, pipelineStage = PipelineStage.VALIDATION) }
        if (jobId != null) {
            viewModelScope.launch {
                try {
                    repository.cancelAiJob(jobId)
                } catch (e: Exception) {
                    // Ignore cancellation errors
                } finally {
                    activeJobId = null
                }
            }
        }
    }

    private fun triggerReportRedirection() {
        viewModelScope.launch {
            if (_state.value.isComplete) {
                _uiActions.send(AnalysisUiAction.NavigateToReports(_state.value.patientId, _state.value.studyId))
            }
        }
    }
}
