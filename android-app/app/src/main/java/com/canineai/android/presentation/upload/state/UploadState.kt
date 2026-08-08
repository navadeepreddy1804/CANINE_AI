package com.canineai.android.presentation.upload.state

data class UploadState(
    // Queue and upload lifecycle status
    val uploadState: UploadStatus = UploadStatus.IDLE,
    val progress: Float = 0f,
    val fileName: String = "",
    val fileSize: String = "",
    val uri: android.net.Uri? = null,
    val itemsInQueue: List<QueueItem> = emptyList(),
    
    // Selected patient details EMR link
    val patientId: String = "",
    val patientName: String = "",
    
    // DICOM Validation metadata parsed results
    val dicomMetadata: DicomMetadata? = null,
    val validationErrors: List<String> = emptyList(),
    
    // UI controls
    val showValidationWarning: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val selectedTab: Int = 0, // 0 = Preview, 1 = Metadata Viewer
    val apiError: String? = null
)

enum class UploadStatus {
    IDLE, UPLOADING, VALIDATING, COMPLETED, FAILED, CANCELLED
}

data class QueueItem(
    val id: String,
    val name: String,
    val size: String,
    val status: UploadStatus,
    val progress: Float
)

data class DicomMetadata(
    val patientId: String,
    val patientName: String,
    val studyUid: String,
    val seriesUid: String,
    val modality: String = "CT",
    val manufacturer: String = "Carestream Dental",
    val dimensions: String = "512 x 512 x 360",
    val sliceThickness: String = "0.300 mm",
    val voxelSize: String = "0.3 x 0.3 x 0.3 mm",
    val sliceCount: Int = 360,
    val studyDate: String = "2026-07-10",
    val studyDescription: String = "Maxilla Cone Beam CT (CBCT)"
)

data class DicomTagItem(
    val groupElement: String,
    val tagDescription: String,
    val vr: String,
    val value: String
)
