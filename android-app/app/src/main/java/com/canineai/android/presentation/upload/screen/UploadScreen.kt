package com.canineai.android.presentation.upload.screen

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.upload.components.UploadProgressCard
import com.canineai.android.presentation.upload.event.UploadEvent
import com.canineai.android.presentation.upload.state.UploadStatus
import com.canineai.android.presentation.upload.viewmodel.UploadViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onNavigateToCbctPreview: (patientId: String, studyId: String) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToPatients: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        var displayName = uri.lastPathSegment ?: "CBCT study"
        var sizeBytes = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { sizeBytes = cursor.getLong(it) }
            }
        }
        viewModel.onEvent(UploadEvent.FileSelected(displayName, formatFileSize(sizeBytes), uri))
    }

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "upload",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = { /* Already on Upload */ },
        onNavigateToAnalysis = onNavigateToAnalysis,
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onLogout = onLogout
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Prepare DICOM / CBCT Study") },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    }
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "upload",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = onNavigateToPatients,
                    onNavigateToUpload = { /* Already on Upload */ },
                    onNavigateToAnalysis = onNavigateToAnalysis,
                    onNavigateToReports = onNavigateToReports
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. REDESIGN PATIENT SELECTION AREA: Compact Card (height <= 60dp)
                var isPatientDropdownExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPatientDropdownExpanded = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PATIENT RECORD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val selected = state.selectedPatientItem
                                if (selected != null) {
                                    val displayId = if (selected.id.startsWith("PT-")) selected.id else "PT-${selected.id}"
                                    Text(
                                        text = "${selected.fullName}  •  ID: $displayId ${if (selected.phone.isNotBlank()) " • ${selected.phone}" else ""}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = state.patientName.ifBlank { "Tap to Select Patient from EMR Registry..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Patient", tint = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(
                            expanded = isPatientDropdownExpanded,
                            onDismissRequest = { isPatientDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            if (state.patientsList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No admitted patients found.") },
                                    onClick = { isPatientDropdownExpanded = false }
                                )
                            } else {
                                state.patientsList.forEach { patient ->
                                    val displayId = if (patient.id.startsWith("PT-")) patient.id else "PT-${patient.id}"
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(patient.fullName, fontWeight = FontWeight.Bold)
                                                    Text("ID: $displayId • Phone: ${patient.phone}", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Text("${patient.age} Y / ${patient.gender}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            viewModel.onEvent(UploadEvent.LinkPatient(patient.id, patient.fullName))
                                            isPatientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CBCT File Picker Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.large)
                        .clickable {
                            filePicker.launch(arrayOf(
                                "application/dicom",
                                "application/zip",
                                "application/gzip",
                                "application/octet-stream"
                            ))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Choose CBCT Scan File", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Tap to browse DICOM (.dcm), NIfTI (.nii, .nii.gz), or ZIP study", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                // Supported format badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supported Formats:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    listOf(".DCM", ".NII", ".NII.GZ", ".ZIP").forEach { format ->
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = format,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                state.apiError?.let { error ->
                    CanineStatusChip(
                        text = "Upload failed. The CBCT could not be processed by the server (${if (error.contains("500")) "HTTP 500 Internal Error" else error})",
                        status = CanineStatus.ERROR,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.onEvent(UploadEvent.DismissError) }
                    )
                }

                // File Upload Progress Card
                if (state.fileName.isNotBlank()) {
                    UploadProgressCard(
                        fileName = state.fileName,
                        fileSize = state.fileSize,
                        status = state.uploadState,
                        progress = state.progress,
                        onCancel = { viewModel.onEvent(UploadEvent.CancelCurrentUpload) },
                        onRetry = { viewModel.onEvent(UploadEvent.TriggerUpload) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Triggers
                if (state.uploadState == UploadStatus.IDLE && state.fileName.isNotBlank()) {
                    CanineButton(
                        text = "Upload Scan to EMR",
                        onClick = { viewModel.onEvent(UploadEvent.TriggerUpload) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.uploadState == UploadStatus.COMPLETED) {
                    val pId = state.patientId.ifBlank { "0" }
                    val sId = state.dicomMetadata?.studyUid.orEmpty().ifBlank { "0" }
                    CanineButton(
                        text = "Preview & Verify Scan",
                        onClick = { onNavigateToCbctPreview(pId, sId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> "Unknown size"
    bytes < 1024L * 1024L -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024f * 1024f))
}
