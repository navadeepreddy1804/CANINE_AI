package com.canineai.android.data.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class PagedResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("pageNumber") val pageNumber: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("lastPage") val lastPage: Boolean
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class GoogleAuthRequest(
    @SerializedName("idToken") val idToken: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("username") val username: String
)

data class LoginResponse(
    // Nullable at the wire boundary so malformed server responses are handled
    // before they reach SessionManager.
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("user") val user: UserDto?
)

data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class UserDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("username") val username: String?,        // null for newly registered users
    @SerializedName("email") val email: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("roleTitle") val roleTitle: String?,      // null until profile is filled in
    @SerializedName("role") val role: String? = null,
    @SerializedName("hospital") val hospital: String?,        // null until profile is filled in
    @SerializedName("department") val department: String?,    // null until profile is filled in
    @SerializedName("medicalRegistrationNumber") val medicalRegistrationNumber: String?,
    @SerializedName("yearsOfExperience") val yearsOfExperience: Int?,
    @SerializedName("bloodGroup") val bloodGroup: String?,
    @SerializedName("enabled") val enabled: Boolean? = false,
    @SerializedName("roles") val roles: List<String>? = emptyList(),
    @SerializedName("profileComplete") val profileComplete: Boolean = false
)

data class DashboardStatsDto(
    @SerializedName("totalPatients") val totalPatients: Int,
    @SerializedName("completedUploads") val completedUploads: Int,
    @SerializedName("totalReports") val totalReports: Int,
    @SerializedName("retrainingQueue") val retrainingQueue: Int,
    @SerializedName("activities") val activities: List<ActivityDto>
)

data class ActivityDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("time") val time: String
)

data class PatientDto(
    @SerializedName("id") val id: String?,
    @SerializedName("hospitalPatientId") val hospitalPatientId: String?,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("age") val age: Int,
    @SerializedName("gender") val gender: String,
    @SerializedName("dateOfBirth") val dob: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("status") val status: String?,
    @SerializedName("orthodontist") val orthodontist: String?,
    @SerializedName("bloodGroup") val bloodGroup: String?,
    @SerializedName("medicalNotes") val medicalNotes: String?,
    @SerializedName("registrationDate") val registrationDate: String?,
    @SerializedName("studies") val studies: List<StudyDto>?,
    @SerializedName("reports") val reports: List<ReportDto>?,
    @SerializedName("hospital") val hospital: String? = null,
    @SerializedName("address") val address: String? = null
)

data class StudyDto(
    @SerializedName("id") val id: String,
    @SerializedName("patientId") val patientId: String,
    @SerializedName("studyInstanceUid") val studyInstanceUid: String?,
    @SerializedName("studyDate") val studyDate: String?,
    @SerializedName("studyTime") val studyTime: String?,
    @SerializedName("modality") val modality: String?,
    @SerializedName("studyDescription") val studyDescription: String?,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("voxelSize") val voxelSize: String?,
    @SerializedName("pixelSpacing") val pixelSpacing: String?,
    @SerializedName("sliceThickness") val sliceThickness: String?,
    @SerializedName("rows") val rows: Int?,
    @SerializedName("columns") val columns: Int?,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("sliceCount") val sliceCount: Int
)

data class UploadSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("patientId") val patientId: String,
    @SerializedName("totalSize") val totalSize: Long,
    @SerializedName("totalFiles") val totalFiles: Int,
    @SerializedName("uploadedSize") val uploadedSize: Long,
    @SerializedName("uploadedFiles") val uploadedFiles: Int,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("expiresAt") val expiresAt: String?
)

data class ReportDto(
    @SerializedName("id") val id: String?,
    @SerializedName("studyId") val studyId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reportStyle") val reportStyle: String?,
    @SerializedName("reportMarkdown") val reportMarkdown: String?,
    @SerializedName("templateVersion") val templateVersion: String?,
    @SerializedName("generationLatencyMs") val generationLatencyMs: Long?,
    @SerializedName("approvedAt") val approvedAt: String?,
    @SerializedName("patientId") val patientId: String? = null,
    @SerializedName("patientName") val patientName: String? = null,
    @SerializedName("patientDisplayId") val patientDisplayId: String? = null,
    @SerializedName("studyDate") val studyDate: String? = null,
    @SerializedName("prediction") val prediction: String? = null,
    @SerializedName("confidence") val confidence: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("rootResorptionRisk") val rootResorptionRisk: String? = null,
    @SerializedName("clinicalRecommendation") val clinicalRecommendation: String? = null,
    @SerializedName("aiResultJson") val aiResultJson: String? = null,
    @SerializedName("canineToothName") val canineToothName: String? = null,
    @SerializedName("canineFdi") val canineFdi: String? = null,
    @SerializedName("canineSector") val canineSector: String? = null,
    @SerializedName("canineVolumeMm3") val canineVolumeMm3: Float? = null,
    @SerializedName("canineAngulation") val canineAngulation: Float? = null,
    @SerializedName("canineCentroid") val canineCentroid: String? = null,
    @SerializedName("totalTeethCount") val totalTeethCount: Int? = null,
    @SerializedName("maxillaryTeethCount") val maxillaryTeethCount: Int? = null,
    @SerializedName("mandibularTeethCount") val mandibularTeethCount: Int? = null,
    @SerializedName("boundingBoxSliceIndex") val boundingBoxSliceIndex: Int? = null,
    @SerializedName("boundingBoxX") val boundingBoxX: Float? = null,
    @SerializedName("boundingBoxY") val boundingBoxY: Float? = null,
    @SerializedName("boundingBoxWidth") val boundingBoxWidth: Float? = null,
    @SerializedName("boundingBoxHeight") val boundingBoxHeight: Float? = null
)

data class AnalysisDto(
    @SerializedName("angle") val angle: String,
    @SerializedName("angleValue") val angleValue: Float,
    @SerializedName("confidence") val confidence: String,
    @SerializedName("observation") val observation: String,
    @SerializedName("recommendations") val recommendations: String,
    @SerializedName("threatLevel") val threatLevel: String
)

data class SettingsDto(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String,
    @SerializedName("hospital") val hospital: String,
    @SerializedName("department") val department: String,
    @SerializedName("medicalRegNo") val medicalRegNo: String,
    @SerializedName("darkMode") val darkMode: Boolean,
    @SerializedName("soundAlerts") val soundAlerts: Boolean,
    @SerializedName("language") val language: String,
    @SerializedName("aiEngine") val aiEngine: String,
    @SerializedName("modelVersion") val modelVersion: String,
    @SerializedName("inferenceTimeout") val inferenceTimeout: Int,
    @SerializedName("maxStorageGb") val maxStorageGb: Float
)

data class AiJobResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("studyId") val studyId: String,
    @SerializedName("taskType") val taskType: String,
    @SerializedName("state") val state: String,
    @SerializedName("activeModelName") val activeModelName: String?,
    @SerializedName("modelVersion") val modelVersion: String?,
    @SerializedName("progressPercentage") val progressPercentage: Int,
    @SerializedName("resultJson") val resultJson: String?,
    @SerializedName("errorMessage") val errorMessage: String?
)

data class AiProgressResponseDto(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("state") val state: String,
    @SerializedName("progressPercentage") val progressPercentage: Int,
    @SerializedName("currentStage") val currentStage: String?,
    @SerializedName("currentModel") val currentModel: String?,
    @SerializedName("timeRemainingSeconds") val timeRemainingSeconds: Int?,
    @SerializedName("gpuUsagePercent") val gpuUsagePercent: Int?,
    @SerializedName("cpuUsagePercent") val cpuUsagePercent: Int?,
    @SerializedName("errorMessage") val errorMessage: String? = null
)
