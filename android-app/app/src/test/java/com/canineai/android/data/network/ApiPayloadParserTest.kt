package com.canineai.android.data.network

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ApiPayloadParserTest {

    @Test
    fun shouldParseWrappedApiResponse() {
        val json = JsonParser.parseString(
            """
            {
              "success": true,
              "message": "Authentication successful",
              "data": {
                "accessToken": "abc",
                "refreshToken": "def",
                "user": {
                  "id": 1,
                  "email": "dr@example.com",
                  "username": "dr",
                  "fullName": "Dr Example",
                  "phone": "+123",
                  "roleTitle": "Orthodontist",
                  "role": "ORTHODONTIST",
                  "hospital": "Metro",
                  "department": "Dentistry",
                  "medicalRegistrationNumber": "MD-1",
                  "yearsOfExperience": 5,
                  "bloodGroup": "O+",
                  "enabled": true,
                  "roles": ["ROLE_ORTHODONTIST"]
                }
              }
            }
            """.trimIndent()
        )

        val parsed = ApiPayloadParser.parse<LoginResponse>(Response.success(json))

        assertEquals("abc", parsed.accessToken)
        assertEquals("Dr Example", parsed.user?.fullName)
        assertEquals("ORTHODONTIST", parsed.user?.role)
    }

    @Test
    fun shouldParseBarePayloadWithoutWrapper() {
        val json = JsonParser.parseString(
            """
            {
              "totalPatients": 3,
              "completedUploads": 2,
              "totalReports": 1,
              "retrainingQueue": 0,
              "activities": []
            }
            """.trimIndent()
        )

        val parsed = ApiPayloadParser.parse<DashboardStatsDto>(Response.success(json))

        assertEquals(3, parsed.totalPatients)
        assertEquals(2, parsed.completedUploads)
        assertEquals(1, parsed.totalReports)
    }
}
