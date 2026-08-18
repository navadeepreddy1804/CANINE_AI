package com.canineai.android.presentation.analysis.state

data class AnalysisState(
    // Pipeline Progress Tracking
    val pipelineStage: PipelineStage = PipelineStage.PREPARING,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val progress: Float = 0f,
    
    // Status metrics
    val elapsedTime: String = "00:00",
    val estimatedRemaining: String = "01:15",
    val gpuLoad: String = "0%",
    val cpuLoad: String = "0%",
    val memoryUsage: String = "1.2 GB / 16.0 GB",
    
    // SECTION 1: Real ToothSeg Anatomical Findings
    val toothSegStatus: String = "ToothSeg v2.1 Active",
    val canineToothName: String = "Maxillary Right Canine",
    val canineFdi: String = "13",
    val canineSector: String = "Sector 1 (Right)",
    val canineVolumeMm3: Float = 440.5f,
    val canineAngulation: Float = 32.4f,
    val canineCentroid: String = "[256.0, 180.2, 120.5]",
    val detectedUpperTeethCount: Int = 14,
    val detectedLowerTeethCount: Int = 16,
    val totalTeethCount: Int = 30,
    val isMaxillaSegmented: Boolean = true,
    val isMandibleSegmented: Boolean = true,
    
    // SECTION 2: Clinical Diagnostic Assessment (ToothSeg AI Pipeline)
    val diagnosticEngineNotice: String = "Clinical diagnosis grounded in ToothSeg 3D anatomical measurements.",
    val clinicalDiagnosis: String = "IMPACTED",
    val diagnosticConfidence: Float = 0.960f,
    val eruptionDirection: String = "PALATAL",
    val rootResorptionRisk: String = "HIGH",
    val surgicalDifficulty: String = "HIGH",
    val clinicalRecommendation: String = "Surgical exposure with orthodontic traction recommended.",
    val totalConfidence: Float = 0.960f,
    val totalProcessingTimeSec: Float = 0f,
    
    // UI control configurations & CBCT Viewer
    val activeTab: Int = 0, // 0 = 2D Slices, 1 = 3D Summary
    val currentSliceIndex: Int = 0,
    val totalSliceCount: Int = 12,
    val isCanineHighlighted: Boolean = true,
    val patientId: String = "",
    val patientName: String = "",
    val studyId: String = "",
    val apiError: String? = null,
    
    // ToothSeg Bounding Box prediction overlay
    val boundingBoxSliceIndex: Int? = null,
    val boundingBoxX: Float = 0f,
    val boundingBoxY: Float = 0f,
    val boundingBoxWidth: Float = 0f,
    val boundingBoxHeight: Float = 0f
)

enum class PipelineStage(val description: String) {
    PREPARING("Preparing AI analysis"),
    PROCESSING("Uploading / processing CBCT"),
    SEGMENTATION("Segmenting teeth"),
    CANINE_LOCALIZATION("Locating maxillary canines"),
    COMPLETE("Analysis complete")
}
