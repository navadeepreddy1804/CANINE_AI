package com.canineai.android.data.repository

import com.canineai.android.data.local.SessionManager
import com.canineai.android.data.network.ApiResponse
import com.canineai.android.data.network.CanineApiService
import com.canineai.android.data.network.ReportDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class CanineRepositoryReportTest {
    private val api: CanineApiService = mock()
    private val sessionManager: SessionManager = mock()
    private val repository = CanineRepository(api, sessionManager)
    private val report = ReportDto(
        id = "report-1", studyId = "study-1", status = "COMPLETED", reportStyle = "CLINICAL",
        reportMarkdown = "Persisted report", templateVersion = "v1", generationLatencyMs = 10,
        approvedAt = "2026-07-27T10:00:00"
    )

    @Test
    fun loadsReportsAndDetailsByReportId() = runTest {
        whenever(api.getReports()).thenReturn(ApiResponse(true, null, listOf(report)))
        whenever(api.getReport("report-1")).thenReturn(ApiResponse(true, null, report))

        assertEquals(listOf(report), repository.getReports())
        assertEquals(report, repository.getReport("report-1"))
        verify(api).getReports()
        verify(api).getReport("report-1")
    }

    @Test
    fun downloadsPersistedPdfByReportId() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        whenever(api.downloadReportPdf("report-1")).thenReturn(
            Response.success(bytes.toResponseBody("application/pdf".toMediaType()))
        )

        assertArrayEquals(bytes, repository.downloadReportPdf("report-1"))
        verify(api).downloadReportPdf("report-1")
    }

    @Test
    fun logoutClearsSessionAndReportCache() = runTest {
        whenever(api.getReports()).thenReturn(ApiResponse(true, null, listOf(report)))
        repository.getReports()
        repository.logout()
        repository.getReports()

        verify(sessionManager).clearSession()
        verify(api, org.mockito.kotlin.times(2)).getReports()
    }
}
