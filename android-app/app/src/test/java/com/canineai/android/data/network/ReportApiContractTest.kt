package com.canineai.android.data.network

import com.canineai.android.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.http.GET

class ReportApiContractTest {
    @Test
    fun exposesOnlyPersistedReportReadEndpoints() {
        val methods = CanineApiService::class.java.methods
        assertEquals("reports", methods.first { it.name == "getReports" }.getAnnotation(GET::class.java).value)
        assertEquals("reports/{reportId}", methods.first { it.name == "getReport" }.getAnnotation(GET::class.java).value)
        assertEquals("reports/{reportId}/pdf", methods.first { it.name == "downloadReportPdf" }.getAnnotation(GET::class.java).value)
        assertTrue(methods.none { it.name == "addReport" })
    }

    @Test
    fun attachesBearerTokenToReportRequests() {
        val sessionManager: SessionManager = mock()
        whenever(sessionManager.getAccessToken()).thenReturn("doctor-token")
        val chain: Interceptor.Chain = mock()
        val request = Request.Builder().url("https://example.test/reports/report-1").build()
        whenever(chain.request()).thenReturn(request)
        whenever(chain.proceed(org.mockito.kotlin.any())).thenReturn(
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        )

        AuthInterceptor(sessionManager).intercept(chain)

        val requestCaptor = argumentCaptor<Request>()
        verify(chain).proceed(requestCaptor.capture())
        assertEquals("Bearer doctor-token", requestCaptor.firstValue.header("Authorization"))
    }
}
