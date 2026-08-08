package com.canineai.android.presentation.workflow

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.canineai.android.data.network.LoginRequest
import com.canineai.android.data.network.LoginResponse
import com.canineai.android.data.network.UserDto
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.auth.event.LoginEvent
import com.canineai.android.presentation.auth.viewmodel.LoginViewModel
import com.canineai.android.presentation.upload.event.UploadEvent
import com.canineai.android.presentation.upload.viewmodel.UploadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowValidationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val repository: CanineRepository = mock()
    private val context: android.content.Context = mock()
    private val contentResolver: android.content.ContentResolver = mock()
    private val uri: android.net.Uri = mock()
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var uploadViewModel: UploadViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.openInputStream(uri)).thenReturn(java.io.ByteArrayInputStream("dummy zip content".toByteArray()))
        loginViewModel = LoginViewModel(repository)
        uploadViewModel = UploadViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldValidateMobileLoginAndUploadWorkflow() = runTest {
        val user = UserDto(
            id = 1L,
            username = "drjane",
            email = "doctor@example.com",
            fullName = "Dr. Jane Doe",
            phone = "+1 555-0000",
            roleTitle = "Orthodontist",
            hospital = "Metro Dental Diagnostics",
            department = "Orthodontics",
            medicalRegistrationNumber = "DENT-123",
            yearsOfExperience = 12,
            bloodGroup = "O+",
            enabled = true,
            roles = listOf("USER")
        )

        whenever(repository.login(LoginRequest("doctor@example.com", "s3cr3t!")))
            .thenReturn(LoginResponse("token", "refresh", user))

        whenever(repository.uploadZip(org.mockito.kotlin.eq("pt-1"), org.mockito.kotlin.any()))
            .thenReturn(com.canineai.android.data.network.UploadSessionDto(
                id = "session-100",
                patientId = "pt-1",
                totalSize = 1000L,
                totalFiles = 1,
                uploadedSize = 1000L,
                uploadedFiles = 1,
                status = "COMPLETED",
                createdAt = "2026-08-06",
                expiresAt = null
            ))

        loginViewModel.onEvent(LoginEvent.EmailChanged("doctor@example.com"))
        loginViewModel.onEvent(LoginEvent.PasswordChanged("s3cr3t!"))
        loginViewModel.onEvent(LoginEvent.SubmitLogin)

        advanceUntilIdle()

        val loginState = loginViewModel.state.value
        assertTrue(loginState.isFormValid)
        assertEquals(false, loginState.isLoading)

        uploadViewModel.onEvent(UploadEvent.LinkPatient("pt-1", "Jane Doe"))
        uploadViewModel.onEvent(UploadEvent.FileSelected("CBCT_Study.zip", "1.2 MB", uri))
        uploadViewModel.onEvent(UploadEvent.TriggerUpload)

        advanceUntilIdle()

        val uploadState = uploadViewModel.state.value
        assertEquals("pt-1", uploadState.patientId)
        assertEquals("CBCT_Study.zip", uploadState.fileName)
        assertTrue(uploadState.uploadState.name in setOf("COMPLETED", "VALIDATING", "UPLOADING"))
    }
}
