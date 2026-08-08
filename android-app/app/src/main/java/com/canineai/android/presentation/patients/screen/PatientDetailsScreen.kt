package com.canineai.android.presentation.patients.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineDialog
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.components.CanineLoadingState
import com.canineai.android.presentation.components.CanineErrorState
import com.canineai.android.presentation.patients.event.PatientEvent
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.patients.state.PatientScanItem
import com.canineai.android.presentation.patients.state.PatientTimelineItem
import com.canineai.android.presentation.patients.viewmodel.PatientViewModel

// Helper function to format Patient ID to enterprise format (PT-00001)
private fun formatPatientId(id: String): String {
    return try {
        val num = id.toInt()
        String.format("PT-%05d", num)
    } catch (e: Exception) {
        if (id.startsWith("PT-")) id else "PT-$id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailsScreen(
    patientId: String,
    viewModel: PatientViewModel,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToUpload: (String) -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
    onNavigateToReports: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val patient = state.selectedPatient

    LaunchedEffect(patientId) {
        viewModel.onEvent(PatientEvent.SelectPatient(patientId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.fullName ?: "EMR Profile") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    CanineIconButton(icon = Icons.Default.Edit, onClick = { patient?.let { onNavigateToEdit(it.id) } }, contentDescription = "Edit Profile")
                    CanineIconButton(icon = Icons.Default.Delete, onClick = { viewModel.onEvent(PatientEvent.DeletePatientRequested) }, contentDescription = "Delete Patient")
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoading) {
            CanineLoadingState(
                message = "Loading Patient EMR Data...",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else if (state.apiError != null && patient == null) {
            CanineErrorState(
                message = state.apiError ?: "Unable to load patient details.",
                onRetry = {
                    viewModel.onEvent(PatientEvent.DismissError)
                    viewModel.onEvent(PatientEvent.SelectPatient(patientId))
                },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else if (patient == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No patient details loaded.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Action error display
                state.apiError?.let { err ->
                    CanineStatusChip(
                        text = err,
                        status = CanineStatus.ERROR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onEvent(PatientEvent.DismissError) }
                    )
                }
                // Profile Card
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = patient.fullName.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = patient.fullName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "EMR ID: ${formatPatientId(patient.id)} • ${patient.gender} • ${patient.age} yrs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Quick Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanineButton(
                        text = "Upload Scan",
                        onClick = { onNavigateToUpload(patient.id) },
                        type = CanineButtonType.OUTLINED,
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    )
                    CanineButton(
                        text = "AI Run",
                        onClick = { onNavigateToAnalysis(patient.id) },
                        type = CanineButtonType.OUTLINED,
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f)
                    )
                    CanineButton(
                        text = "Report",
                        onClick = { onNavigateToReports(patient.id) },
                        type = CanineButtonType.OUTLINED,
                        icon = Icons.Default.MailOutline,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Future CBCT Interactive 3D Orthodontic Viewer Placeholder
                Text(
                    "CBCT preview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3D Scan Spatial Overlay Mode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            CanineStatusChip(text = "AI View Mode Active", status = CanineStatus.INFO)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Four-pane diagnostic viewer grid
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ViewerPane(title = "AXIAL (X-Y)", subtitle = "Tooth Segmentation Overlay", modifier = Modifier.weight(1f))
                            ViewerPane(title = "CORONAL (X-Z)", subtitle = "Landmarks & Crosshairs", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ViewerPane(title = "SAGITTAL (Y-Z)", subtitle = "Canine Angle Measurement", modifier = Modifier.weight(1f))
                            ViewerPane(title = "3D RECONSTRUCTION", subtitle = "Volumetric Surface Model", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // EMR Details Block
                Text(
                    "Patient Demographics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    DemographicRow(label = "Date of Birth", value = patient.dob)
                    DemographicRow(label = "Blood Group", value = patient.bloodGroup ?: "N/A")
                    DemographicRow(label = "Phone Number", value = patient.phone)
                    DemographicRow(label = "Email Address", value = patient.email)
                    DemographicRow(label = "Orthodontist", value = patient.orthodontist)
                    DemographicRow(label = "Register Date", value = patient.registrationDate)
                    DemographicRow(label = "Medical Notes", value = patient.medicalNotes, isMultiLine = true)
                }

                // Uploaded Scan studies
                Text(
                    "Imaging & Diagnostic Studies",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (state.patientScans.isEmpty()) {
                    CanineEmptyState(
                        title = "No CBCT Scans",
                        message = "This patient does not have any recorded CBCT scans.",
                        icon = Icons.Default.Share,
                        actionText = "Upload Scan",
                        onActionClick = { onNavigateToUpload(patient.id) }
                    )
                } else {
                    state.patientScans.forEach { scan ->
                        ScanRowCard(scan = scan)
                    }
                }

                // Patient history timeline
                Text(
                    "Clinical History Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.patientTimeline.forEach { item ->
                        TimelineRowItem(item = item)
                    }
                }
            }
        }
    }

    // Delete confirm dialogue
    if (state.showDeleteConfirmation) {
        CanineDialog(
            title = "Delete Patient Profile",
            message = "Are you sure you want to schedule removal for ${patient?.fullName}? This EMR record will be marked for soft deletion and fully removed after 72 hours unless restored.",
            confirmButtonText = "Schedule Deletion",
            onConfirm = { viewModel.onEvent(PatientEvent.DeletePatientConfirmed) },
            onDismissRequest = { viewModel.onEvent(PatientEvent.DismissDeleteDialog) },
            dismissButtonText = "Cancel",
            isDestructive = true
        )
    }
}

@Composable
private fun ViewerPane(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DemographicRow(label: String, value: String, isMultiLine: Boolean = false) {
    if (isMultiLine) {
        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
}

@Composable
private fun ScanRowCard(scan: PatientScanItem) {
    CanineCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = scan.studyName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "${scan.date} • ${scan.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            CanineStatusChip(text = scan.analysisStatus, status = CanineStatus.SUCCESS)
        }
    }
}

@Composable
private fun TimelineRowItem(item: PatientTimelineItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = item.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = item.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
