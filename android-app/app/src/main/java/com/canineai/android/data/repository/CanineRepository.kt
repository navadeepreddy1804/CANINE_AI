package com.canineai.android.data.repository

import com.canineai.android.data.local.SessionManager
import com.canineai.android.data.network.*
import com.canineai.android.presentation.patients.state.PatientDetails
import retrofit2.Response
import com.canineai.android.presentation.patients.state.PatientItem
import com.canineai.android.presentation.patients.state.PatientScanItem
import com.canineai.android.presentation.patients.state.PatientTimelineItem
import com.canineai.android.presentation.theme.ThemeManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanineRepository @Inject constructor(
    private val apiService: CanineApiService,
    private val sessionManager: SessionManager
) {
    private var cachedReports: List<ReportDto> = emptyList()
    private val cachedReportDetails = mutableMapOf<String, ReportDto>()

    private fun <T> handleResponse(response: ApiResponse<T>): T {
        if (response.success) {
            @Suppress("UNCHECKED_CAST")
            return response.data as T
        } else {
            throw Exception(response.message ?: "Unknown API Error")
        }
    }

    private fun <T> handleRawPayload(payload: Any?, type: Class<T>): T {
        if (payload == null) {
            throw Exception("Empty API response")
        }

        return when (payload) {
            is Map<*, *> -> {
                val json = com.google.gson.Gson().toJsonTree(payload)
                com.google.gson.Gson().fromJson(json, type)
            }
            is List<*> -> {
                val json = com.google.gson.Gson().toJsonTree(payload)
                com.google.gson.Gson().fromJson(json, type)
            }
            else -> {
                val json = payload.toString()
                val parsed = com.google.gson.JsonParser.parseString(json)
                com.google.gson.Gson().fromJson(parsed, type)
            }
        }
    }

    suspend fun login(req: LoginRequest): LoginResponse {
        val loginResponse = handleResponse(apiService.login(req))
        val accessToken = loginResponse.accessToken?.trim().orEmpty()
        val refreshToken = loginResponse.refreshToken?.trim().orEmpty()
        val user = loginResponse.user
            ?: throw IllegalStateException("Login response did not include a user profile")
        val email = user.email?.trim().orEmpty()
        val fullName = user.fullName?.trim().orEmpty()
        if (accessToken.isEmpty() || refreshToken.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            throw IllegalStateException("Login response is missing required session details")
        }
        val role = user.roleTitle?.trim().takeUnless { it.isNullOrEmpty() }
            ?: user.role?.trim().takeUnless { it.isNullOrEmpty() }
            ?: user.roles?.firstOrNull()?.removePrefix("ROLE_")
            ?: "Clinician"
        /*
        sessionManager.saveSession(
            accessToken  = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            email        = loginResponse.user.email.orEmpty(),
            fullName     = loginResponse.user.fullName.orEmpty(),
            role         = loginResponse.user.roleTitle   // nullable — SessionManager handles null
        )
        */
        clearReportCache()
        sessionManager.saveSession(accessToken, refreshToken, email, fullName, role)
        ThemeManager.activateProfile(email)
        return loginResponse
    }

    suspend fun googleLogin(req: GoogleAuthRequest): LoginResponse {
        val loginResponse = handleResponse(apiService.googleLogin(req))
        val accessToken = loginResponse.accessToken?.trim().orEmpty()
        val refreshToken = loginResponse.refreshToken?.trim().orEmpty()
        val user = loginResponse.user
            ?: throw IllegalStateException("Login response did not include a user profile")
        val email = user.email?.trim().orEmpty()
        val fullName = user.fullName?.trim().orEmpty()
        if (accessToken.isEmpty() || refreshToken.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            throw IllegalStateException("Login response is missing required session details")
        }
        val role = user.roleTitle?.trim().takeUnless { it.isNullOrEmpty() }
            ?: user.role?.trim().takeUnless { it.isNullOrEmpty() }
            ?: user.roles?.firstOrNull()?.removePrefix("ROLE_")
            ?: "Clinician"
            
        clearReportCache()
        sessionManager.saveSession(accessToken, refreshToken, email, fullName, role)
        ThemeManager.activateProfile(email)
        return loginResponse
    }

    suspend fun register(req: RegisterRequest): UserDto {
        return handleResponse(apiService.register(req))
    }

    suspend fun forgotPassword(email: String) {
        handleResponse(apiService.forgotPassword(mapOf("email" to email)))
    }

    suspend fun getCurrentUser(): UserDto {
        return handleResponse(apiService.getCurrentUser())
    }

    suspend fun getDashboardStats(): DashboardStatsDto {
        val patients = getPatients()
        val reports = getReports()
        return DashboardStatsDto(
            totalPatients = patients.size,
            completedUploads = 0,
            totalReports = reports.size,
            retrainingQueue = 0,
            activities = emptyList()
        )
    }

    suspend fun getPatients(
        search: String? = null,
        gender: String? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20
    ): List<PatientItem> {
        val pagedResponse = handleResponse(apiService.getPatients(search, gender, status, page, size))
        return pagedResponse.content.map { dto ->
            PatientItem(
                id = dto.id ?: dto.hospitalPatientId ?: "",
                fullName = dto.fullName.orEmpty(),
                age = dto.age ?: 0,
                gender = dto.gender.orEmpty(),
                phone = dto.phone.orEmpty(),
                email = dto.email.orEmpty(),
                status = dto.status ?: "Active",
                lastAnalysisDate = dto.registrationDate
            )
        }
    }

    suspend fun getPatientDetails(id: String): PatientDetails {
        val dto = handleResponse(apiService.getPatientDetails(id))
        return PatientDetails(
            id = dto.hospitalPatientId ?: dto.id ?: "",
            fullName = dto.fullName.orEmpty(),
            age = dto.age ?: 0,
            gender = dto.gender.orEmpty(),
            dob = dto.dateOfBirth ?: dto.dob.orEmpty(),
            bloodGroup = dto.bloodGroup ?: "O+",
            phone = dto.phone.orEmpty(),
            email = dto.email.orEmpty(),
            address = dto.address ?: "",
            emergencyContact = "Unavailable", // Identified as missing backend capability
            medicalNotes = dto.medicalNotes ?: "",
            orthodontist = dto.orthodontist ?: "",
            hospital = dto.hospital ?: "",
            registrationDate = dto.registrationDate ?: "",
            status = dto.status ?: "ACTIVE"
        )
    }

    suspend fun getPatientScans(id: String): List<PatientScanItem> {
        val studies = handleResponse(apiService.getPatientStudies(id))
        return studies.map { s ->
            PatientScanItem(
                id = s.id,
                studyName = s.studyDescription ?: "CBCT Study",
                date = s.createdAt,
                size = "${s.sliceCount} slices",
                analysisStatus = s.status
            )
        }
    }

    suspend fun getPatientTimeline(id: String): List<PatientTimelineItem> {
        val details = getPatientDetails(id)
        val scans = getPatientScans(id)
        val timeline = mutableListOf<PatientTimelineItem>()
        
        timeline.add(PatientTimelineItem(
            id = "reg",
            title = "Patient Registered",
            subtitle = "Admitted to CanineAI System",
            date = details.registrationDate
        ))
        
        scans.forEach { scan ->
            timeline.add(PatientTimelineItem(
                id = scan.id + "_upload",
                title = "Study Uploaded",
                subtitle = scan.studyName,
                date = scan.date ?: details.registrationDate
            ))
            if (scan.analysisStatus == "COMPLETED" || scan.analysisStatus == "REPORT_GENERATED") {
                timeline.add(PatientTimelineItem(
                    id = scan.id + "_analysis",
                    title = "AI Analysis Completed",
                    subtitle = scan.studyName,
                    date = scan.date ?: details.registrationDate
                ))
            }
            if (scan.analysisStatus == "REPORT_GENERATED") {
                timeline.add(PatientTimelineItem(
                    id = scan.id + "_report",
                    title = "Clinical Report Generated",
                    subtitle = "Available in Reports Library",
                    date = scan.date ?: details.registrationDate
                ))
            }
        }
        return timeline.sortedByDescending { it.date }
    }

    suspend fun savePatient(patient: PatientDto): PatientDto {
        return if (patient.id.isNullOrBlank()) {
            handleResponse(apiService.savePatient(patient))
        } else {
            handleResponse(apiService.updatePatient(patient.id, patient))
        }
    }

    suspend fun deletePatient(id: String) {
        handleResponse(apiService.deletePatient(id))
    }

    suspend fun initializeUploadSession(patientId: String, totalSize: Long, totalFiles: Int): UploadSessionDto {
        return handleResponse(apiService.initializeSession(patientId, totalSize, totalFiles))
    }

    suspend fun uploadChunk(sessionId: String, fileName: String, fileBytes: okhttp3.RequestBody) {
        handleResponse(apiService.uploadChunk(sessionId, fileName, fileBytes))
    }

    suspend fun uploadZip(patientId: String, fileBytes: okhttp3.RequestBody): UploadSessionDto {
        return handleResponse(apiService.uploadZip(patientId, fileBytes))
    }

    suspend fun getSessionStatus(sessionId: String): UploadSessionDto {
        return handleResponse(apiService.getSessionStatus(sessionId))
    }

    suspend fun getAnalysis(id: String): AnalysisDto {
        return handleResponse(apiService.getAnalysis(id))
    }

    suspend fun getReports(): List<ReportDto> {
        return handleResponse(apiService.getReports()).also { cachedReports = it }
    }

    suspend fun getHistory(): List<HistoryDto> {
        return try {
            handleResponse(apiService.getHistory())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReport(reportId: String): ReportDto {
        return handleResponse(apiService.getReport(reportId)).also { cachedReportDetails[reportId] = it }
    }

    suspend fun getReportByStudyId(studyId: String): ReportDto? {
        return try {
            handleResponse(apiService.getReportByStudyId(studyId))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadReportPdf(reportId: String): ByteArray {
        val response = apiService.downloadReportPdf(reportId)
        if (!response.isSuccessful) {
            throw IllegalStateException("Unable to download report PDF (${response.code()})")
        }
        return response.body()?.bytes() ?: throw IllegalStateException("Report PDF was empty")
    }

    fun clearReportCache() {
        cachedReports = emptyList()
        cachedReportDetails.clear()
    }

    fun logout() {
        clearReportCache()
        sessionManager.clearSession()
    }



    suspend fun updateProfile(user: UserDto): UserDto {
        return handleResponse(apiService.updateProfile(user))
    }

    suspend fun submitAiJob(studyId: String): AiJobResponseDto {
        return handleResponse(apiService.submitJob(mapOf("studyId" to studyId, "taskType" to "CBCT_SEGMENTATION")))
    }

    suspend fun getAiJob(jobId: String): AiJobResponseDto {
        return handleResponse(apiService.getJob(jobId))
    }

    suspend fun getAiJobProgress(jobId: String): AiProgressResponseDto {
        return handleResponse(apiService.getJobProgress(jobId))
    }

    suspend fun cancelAiJob(jobId: String) {
        handleResponse(apiService.cancelJob(jobId))
    }
}
