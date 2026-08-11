package com.canineai.android.data.network

import retrofit2.http.*
import okhttp3.ResponseBody
import retrofit2.Response

interface CanineApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    @POST("auth/google")
    suspend fun googleLogin(@Body body: GoogleAuthRequest): ApiResponse<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<UserDto>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): ApiResponse<Unit>

    @GET("auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): ApiResponse<TokenResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body body: UserDto): ApiResponse<UserDto>

    @GET("patients")
    suspend fun getPatients(
        @Query("search") search: String? = null,
        @Query("gender") gender: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedResponse<PatientDto>>

    @GET("patients/{id}")
    suspend fun getPatientDetails(@Path("id") id: String): ApiResponse<PatientDto>

    @POST("patients")
    suspend fun savePatient(@Body patient: PatientDto): ApiResponse<PatientDto>

    @PUT("patients/{id}")
    suspend fun updatePatient(@Path("id") id: String, @Body patient: PatientDto): ApiResponse<PatientDto>

    @DELETE("patients/{id}")
    suspend fun deletePatient(@Path("id") id: String): ApiResponse<Unit>

    @GET("patients/{patientId}/studies")
    suspend fun getPatientStudies(@Path("patientId") patientId: String): ApiResponse<List<StudyDto>>

    @GET("history")
    suspend fun getHistory(): ApiResponse<List<HistoryDto>>

    @POST("uploads")
    suspend fun initializeSession(
        @Query("patientId") patientId: String,
        @Query("totalSize") totalSize: Long,
        @Query("totalFiles") totalFiles: Int
    ): ApiResponse<UploadSessionDto>

    @POST("uploads/{id}/chunk")
    suspend fun uploadChunk(
        @Path("id") id: String,
        @Query("fileName") fileName: String,
        @Body fileBytes: okhttp3.RequestBody
    ): ApiResponse<Unit>

    @POST("uploads/zip")
    suspend fun uploadZip(
        @Query("patientId") patientId: String,
        @Body fileBytes: okhttp3.RequestBody
    ): ApiResponse<UploadSessionDto>

    @GET("uploads/{id}")
    suspend fun getSessionStatus(@Path("id") id: String): ApiResponse<UploadSessionDto>

    @GET("analysis/{id}")
    suspend fun getAnalysis(@Path("id") id: String): ApiResponse<AnalysisDto>

    @GET("reports")
    suspend fun getReports(): ApiResponse<List<ReportDto>>

    @GET("reports/{reportId}")
    suspend fun getReport(@Path("reportId") reportId: String): ApiResponse<ReportDto>

    @GET("reports/study/{studyId}")
    suspend fun getReportByStudyId(@Path("studyId") studyId: String): ApiResponse<ReportDto>

    @Streaming
    @GET("reports/{reportId}/pdf")
    suspend fun downloadReportPdf(@Path("reportId") reportId: String): Response<ResponseBody>

    @POST("ai/jobs")
    suspend fun submitJob(@Body body: Map<String, String>): ApiResponse<AiJobResponseDto>

    @GET("ai/jobs/{id}/status")
    suspend fun getJobProgress(@Path("id") id: String): ApiResponse<AiProgressResponseDto>

    @GET("ai/jobs/{id}")
    suspend fun getJob(@Path("id") id: String): ApiResponse<AiJobResponseDto>

    @POST("ai/jobs/{id}/cancel")
    suspend fun cancelJob(@Path("id") id: String): ApiResponse<Unit>
}
